package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.TransaccionServicio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.PagoServicioRequest;
import com.proyecto.servicios.model.cliente.PagoServicioResponseDto;
import com.proyecto.servicios.model.cliente.TransaccionConsultaRequest;
import com.proyecto.servicios.model.gestopago.tx.TxXmlResponse;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.TransaccionServicioRepository;
import com.proyecto.servicios.specification.TransaccionServicioSpecification;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PagoServicioService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionServicioRepository transaccionRepository;
    private final ProductoMongoRepository productoMongoRepository;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoTxClient txClient;
    private final AesEncryptionUtil aesUtil;
    private final PasswordEncoder passwordEncoder;
    private final JaxbXmlParser jaxbXmlParser;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer defaultDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String defaultCodigoDispositivo;

    @Value("${gestopago.aes.secret-key:GP2026_AES_KEY_!}")
    private String aesSecretKey;

    @Value("${gestopago.auth.token:}")
    private String fallbackToken;

    public PagoServicioService(CuentaRepository cuentaRepository,
                               TransaccionServicioRepository transaccionRepository,
                               ProductoMongoRepository productoMongoRepository,
                               GestoPagoTokenService tokenService,
                               GestoPagoTxClient txClient,
                               AesEncryptionUtil aesUtil,
                               PasswordEncoder passwordEncoder,
                               JaxbXmlParser jaxbXmlParser,
                               ObjectMapper objectMapper) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.productoMongoRepository = productoMongoRepository;
        this.tokenService = tokenService;
        this.txClient = txClient;
        this.aesUtil = aesUtil;
        this.passwordEncoder = passwordEncoder;
        this.jaxbXmlParser = jaxbXmlParser;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PagoServicioResponseDto procesarPagoServicio(PagoServicioRequest request) {
        log.info("Iniciando procesamiento de pago de servicio para cuenta={}, idServicio={}, idProducto={}",
                enmascararCuenta(request.getNumCuenta()), request.getIdServicio(), request.getIdProducto());

        // 1. Validar Cuenta existente y activa
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumCuenta().trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta bancaria " + request.getNumCuenta() + " no existe"));

        if (!Boolean.TRUE.equals(cuenta.getActivo()) || !"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            log.warn("Rechazo de pago: La cuenta {} se encuentra inactiva o bloqueada", request.getNumCuenta());
            throw new ValidacionNegocioException("La cuenta no se encuentra activa para operar.");
        }

        if (cuenta.getCliente() == null || !Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            log.warn("Rechazo de pago: El cliente titular de la cuenta {} está inactivo", request.getNumCuenta());
            throw new ValidacionNegocioException("El cliente titular de la cuenta se encuentra inactivo.");
        }

        // 2. Validar Pass Seguro / PIN con BCrypt
        Usuario usuario = cuenta.getCliente().getUsuario();
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo()) || !passwordEncoder.matches(request.getPin().trim(), usuario.getPassword())) {
            log.warn("Fallo de autenticación: PIN o contraseña incorrecta para autorizar la transacción");
            throw new CredencialesInvalidasException("El PIN o contraseña de autorización es incorrecto.");
        }

        // 3. Validar Saldo preliminar suficiente
        if (cuenta.getSaldo().compareTo(request.getPrecio()) < 0) {
            log.warn("Rechazo por saldo insuficiente: Saldo disponible=${}, Monto=${}", cuenta.getSaldo(), request.getPrecio());
            throw new ValidacionNegocioException("Saldo insuficiente en la cuenta. Saldo disponible: $"
                    + cuenta.getSaldo() + ", Monto requerido: $" + request.getPrecio());
        }

        // 4. Validar existencia y precio contra Catálogo MongoDB
        ProductoDocument productoDoc = productoMongoRepository.findByIdProductoAndIdServicio(request.getIdProducto(), request.getIdServicio())
                .orElseThrow(() -> new ValidacionNegocioException("El producto con ID " + request.getIdProducto()
                        + " no pertenece al servicio " + request.getIdServicio() + " o no existe en el catálogo."));

        if (productoDoc.getPrecio() != null && productoDoc.getPrecio() > 0.0) {
            BigDecimal precioCatalogo = BigDecimal.valueOf(productoDoc.getPrecio());
            if (precioCatalogo.compareTo(request.getPrecio()) != 0) {
                log.warn("Discrepancia de monto: Solicitado=${}, Catálogo=${}", request.getPrecio(), precioCatalogo);
                throw new ValidacionNegocioException("El monto solicitado ($" + request.getPrecio()
                        + ") no coincide con el precio fijo configurado en el catálogo ($" + precioCatalogo + ").");
            }
        }

        // 5. Validar formato según Bandera (Con referencia vs Sin referencia / Tiempo Aire)
        String bandera = request.getBandera().trim().toUpperCase();
        if ("SIN_REFERENCIA".equals(bandera) || "TIEMPO_AIRE".equals(bandera)) {
            if (!request.getTelefonoReferencia().trim().matches("\\d{10}")) {
                throw new ValidacionNegocioException("Para operaciones sin referencia (Tiempo Aire), el número telefónico debe contener exactamente 10 dígitos numéricos.");
            }
        } else if ("CON_REFERENCIA".equals(bandera) || "PAGO_SERVICIO".equals(bandera)) {
            if (request.getTelefonoReferencia().trim().length() < 3) {
                throw new ValidacionNegocioException("La referencia del recibo o servicio debe contener al menos 3 caracteres.");
            }
        }

        // 6. Generar folio aleatorio / randomString para idempotencia
        String randomString = (request.getRandomString() != null && !request.getRandomString().isBlank())
                ? request.getRandomString().trim()
                : "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();

        if (transaccionRepository.existsByRandomString(randomString)) {
            throw new ValidacionNegocioException("Ya existe una transacción registrada con el mismo folio de idempotencia (randomString): " + randomString);
        }

        // 7. Armar Payload para GestoPago sendTx.do
        String horaLocal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String upc = productoDoc.getId() != null ? productoDoc.getId() : String.valueOf(productoDoc.getIdProducto());
        Integer distribuidor = request.getDistribuidor() != null ? request.getDistribuidor() : defaultDistribuidor;

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("idProducto", request.getIdProducto());
        payloadMap.put("idServicio", request.getIdServicio());
        payloadMap.put("telefonoReferencia", request.getTelefonoReferencia().trim());
        payloadMap.put("horaLocal", horaLocal);
        payloadMap.put("montoPago", request.getPrecio().doubleValue());
        payloadMap.put("upc", upc);
        payloadMap.put("unidad", 1);
        payloadMap.put("randomString", randomString);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new ValidacionNegocioException("Error al serializar el payload de la transacción", e);
        }

        // 8. Firmar / Cifrar con AES
        String payloadFirmadoAes = aesUtil.cifrar(jsonPayload, aesSecretKey);

        // 9. Registrar Transacción inicial en PostgreSQL con estatus PENDIENTE
        TransaccionServicio tx = TransaccionServicio.builder()
                .cuenta(cuenta)
                .randomString(randomString)
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .precio(request.getPrecio())
                .telefonoReferencia(request.getTelefonoReferencia().trim())
                .bandera(bandera)
                .distribuidor(distribuidor)
                .horaLocal(horaLocal)
                .payloadFirmadoAes(payloadFirmadoAes)
                .estatus("PENDIENTE")
                .build();

        tx = transaccionRepository.save(tx);

        // 10. Obtener Token Bearer activo de BD
        String bearerToken = "Bearer " + resolverToken();

        // 11. Ejecutar petición a GestoPago (POST sendTx.do)
        String respuestaProveedor;
        try {
            respuestaProveedor = txClient.enviarTransaccionJson(bearerToken, jsonPayload);
        } catch (RetryableException e) {
            log.error("Timeout de red en GestoPago sendTx.do. Transacción marcada en EN_REVISION: {}", e.getMessage());
            tx.setEstatus("EN_REVISION");
            tx.setMensajeProveedor("Timeout de comunicación con el proveedor externo. Operación sujeta a conciliación.");
            transaccionRepository.save(tx);
            throw new GestoPagoIntegrationException("Tiempo de espera agotado al conectar con GestoPago. La transacción quedó EN_REVISION sin descontar saldo.");
        } catch (FeignException e) {
            log.warn("Error HTTP retornado por GestoPago: status={}, body={}", e.status(), e.contentUTF8());
            respuestaProveedor = e.contentUTF8();
            if (respuestaProveedor == null || respuestaProveedor.isBlank()) {
                tx.setEstatus("RECHAZADA");
                tx.setCodigoRespuestaProveedor(String.valueOf(e.status()));
                tx.setMensajeProveedor("Error HTTP " + e.status() + " devuelto por el proveedor");
                transaccionRepository.save(tx);
                throw new GestoPagoIntegrationException("GestoPago rechazó la operación con código HTTP " + e.status());
            }
        }

        // 12. Interpretar la respuesta del proveedor
        String codigoRespuesta = extraerCodigo(respuestaProveedor);
        String textoRespuesta = extraerTexto(respuestaProveedor);
        String folio = extraerFolio(respuestaProveedor);
        String autorizacion = extraerAutorizacion(respuestaProveedor);

        log.info("Respuesta interpretada de GestoPago: codigo={}, mensaje={}", codigoRespuesta, textoRespuesta);

        // 13. REGLA CLAVE DEL PROFESOR: SOLO SI EL CÓDIGO ES "01" SE DESCUENTA EL SALDO
        if ("01".equals(codigoRespuesta)) {
            log.info("Código 01 confirmado por GestoPago. Descontando saldo de la cuenta {}", cuenta.getNumeroCuenta());

            int filas = cuentaRepository.descontarSaldoSiSuficiente(cuenta.getNumeroCuenta(), request.getPrecio());
            if (filas == 0) {
                log.error("Fallo crítico de concurrencia: Saldo insuficiente al momento de aplicar descuento confirmado");
                throw new ValidacionNegocioException("Error de concurrencia: El saldo disponible cambió antes de asentar el cobro.");
            }

            cuenta.setSaldo(cuenta.getSaldo().subtract(request.getPrecio()));
            tx.setEstatus("EXITOSA");
            tx.setCodigoRespuestaProveedor("01");
            tx.setMensajeProveedor(textoRespuesta != null ? textoRespuesta : "Operacion realizada con exito");
            tx.setFolioProveedor(folio != null ? folio : "FOL-" + randomString.substring(randomString.length() - 6));
            tx.setAutorizacionProveedor(autorizacion != null ? autorizacion : "AUT-" + System.currentTimeMillis() % 100000);
        } else {
            log.warn("Transacción no exitosa en GestoPago (código={}). NO se descuenta saldo de la cuenta.", codigoRespuesta);
            tx.setEstatus("RECHAZADA");
            tx.setCodigoRespuestaProveedor(codigoRespuesta != null ? codigoRespuesta : "99");
            tx.setMensajeProveedor(textoRespuesta != null ? textoRespuesta : "Operación no procesada por el proveedor");
        }

        tx = transaccionRepository.save(tx);

        return PagoServicioResponseDto.builder()
                .idTransaccion(tx.getId())
                .randomString(tx.getRandomString())
                .numCuenta(enmascararCuenta(cuenta.getNumeroCuenta()))
                .idServicio(tx.getIdServicio())
                .idProducto(tx.getIdProducto())
                .nombreServicio(productoDoc.getServicio())
                .nombreProducto(productoDoc.getProducto())
                .precio(tx.getPrecio())
                .saldoRestante(cuenta.getSaldo())
                .telefonoReferencia(enmascararReferencia(tx.getTelefonoReferencia()))
                .bandera(tx.getBandera())
                .estatus(tx.getEstatus())
                .codigoProveedor(tx.getCodigoRespuestaProveedor())
                .mensajeProveedor(tx.getMensajeProveedor())
                .folioProveedor(tx.getFolioProveedor())
                .autorizacionProveedor(tx.getAutorizacionProveedor())
                .fechaTransaccion(tx.getFechaCreacion())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PagoServicioResponseDto> consultarTransacciones(TransaccionConsultaRequest request) {
        log.info("Consultando transacciones dinámicas");

        int pagina = (request != null && request.getPagina() != null) ? request.getPagina() : 0;
        int tamano = (request != null && request.getTamanoPagina() != null) ? request.getTamanoPagina() : 20;
        String ordenarPor = (request != null && request.getOrdenarPor() != null && !request.getOrdenarPor().isBlank()) ? request.getOrdenarPor() : "id";
        Sort.Direction direccion = (request != null && "ASC".equalsIgnoreCase(request.getDireccionOrden())) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(direccion, ordenarPor));
        Specification<TransaccionServicio> spec = TransaccionServicioSpecification.conFiltros(request);

        return transaccionRepository.findAll(spec, pageable).map(this::toResponseDto);
    }

    private PagoServicioResponseDto toResponseDto(TransaccionServicio tx) {
        return PagoServicioResponseDto.builder()
                .idTransaccion(tx.getId())
                .randomString(tx.getRandomString())
                .numCuenta(enmascararCuenta(tx.getCuenta().getNumeroCuenta()))
                .idServicio(tx.getIdServicio())
                .idProducto(tx.getIdProducto())
                .nombreServicio("Servicio #" + tx.getIdServicio())
                .nombreProducto("Producto #" + tx.getIdProducto())
                .precio(tx.getPrecio())
                .saldoRestante(tx.getCuenta().getSaldo())
                .telefonoReferencia(enmascararReferencia(tx.getTelefonoReferencia()))
                .bandera(tx.getBandera())
                .estatus(tx.getEstatus())
                .codigoProveedor(tx.getCodigoRespuestaProveedor())
                .mensajeProveedor(tx.getMensajeProveedor())
                .folioProveedor(tx.getFolioProveedor())
                .autorizacionProveedor(tx.getAutorizacionProveedor())
                .fechaTransaccion(tx.getFechaCreacion())
                .build();
    }

    private String resolverToken() {
        Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(defaultDistribuidor, defaultCodigoDispositivo);
        if (tokenOpt.isPresent() && tokenOpt.get().getToken() != null && !tokenOpt.get().getToken().isBlank()) {
            return tokenOpt.get().getToken();
        }
        if (fallbackToken != null && !fallbackToken.isBlank()) {
            return fallbackToken;
        }
        throw new GestoPagoIntegrationException("No existe un token de sesión activo para comunicarse con GestoPago");
    }

    private String extraerCodigo(String raw) {
        if (raw == null) return "99";
        if (raw.trim().startsWith("<")) {
            try {
                TxXmlResponse xml = jaxbXmlParser.unmarshal(raw, TxXmlResponse.class);
                if (xml.getMensaje() != null) return xml.getMensaje().getCodigo();
            } catch (Exception ignored) { }
        } else if (raw.trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                if (root.has("codigo")) return root.get("codigo").asText();
                if (root.has("CODIGO")) return root.get("CODIGO").asText();
            } catch (Exception ignored) { }
        }
        return "99";
    }

    private String extraerTexto(String raw) {
        if (raw == null) return "Sin respuesta";
        if (raw.trim().startsWith("<")) {
            try {
                TxXmlResponse xml = jaxbXmlParser.unmarshal(raw, TxXmlResponse.class);
                if (xml.getMensaje() != null) return xml.getMensaje().getTexto();
            } catch (Exception ignored) { }
        } else if (raw.trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                if (root.has("mensaje")) return root.get("mensaje").asText();
                if (root.has("texto")) return root.get("texto").asText();
                if (root.has("TEXTO")) return root.get("TEXTO").asText();
            } catch (Exception ignored) { }
        }
        return raw;
    }

    private String extraerFolio(String raw) {
        if (raw == null) return null;
        if (raw.trim().startsWith("<")) {
            try {
                TxXmlResponse xml = jaxbXmlParser.unmarshal(raw, TxXmlResponse.class);
                if (xml.getTransaccion() != null) return xml.getTransaccion().getFolio();
            } catch (Exception ignored) { }
        } else if (raw.trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                if (root.has("folio")) return root.get("folio").asText();
                if (root.has("FOLIO")) return root.get("FOLIO").asText();
            } catch (Exception ignored) { }
        }
        return null;
    }

    private String extraerAutorizacion(String raw) {
        if (raw == null) return null;
        if (raw.trim().startsWith("<")) {
            try {
                TxXmlResponse xml = jaxbXmlParser.unmarshal(raw, TxXmlResponse.class);
                if (xml.getTransaccion() != null) return xml.getTransaccion().getAutorizacion();
            } catch (Exception ignored) { }
        } else if (raw.trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                if (root.has("autorizacion")) return root.get("autorizacion").asText();
                if (root.has("AUTORIZACION")) return root.get("AUTORIZACION").asText();
            } catch (Exception ignored) { }
        }
        return null;
    }

    private String enmascararCuenta(String cuenta) {
        if (cuenta == null || cuenta.length() < 16) return "4152********8475";
        return cuenta.substring(0, 4) + "********" + cuenta.substring(cuenta.length() - 4);
    }

    private String enmascararReferencia(String ref) {
        if (ref == null || ref.length() < 4) return "****";
        if (ref.length() == 10) {
            return ref.substring(0, 2) + "******" + ref.substring(8);
        }
        return ref.substring(0, 2) + "****" + ref.substring(ref.length() - 2);
    }
}
