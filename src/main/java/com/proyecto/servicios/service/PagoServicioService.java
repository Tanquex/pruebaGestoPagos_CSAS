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
import com.proyecto.servicios.exception.GestoPagoResultadoInciertoException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.PagoServicioRequest;
import com.proyecto.servicios.model.cliente.PagoServicioResponseDto;
import com.proyecto.servicios.model.cliente.TransaccionConsultaRequest;
import com.proyecto.servicios.model.gestopago.tx.TxDetalleXml;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Orquesta el pago de servicios contra GestoPago en tres pasos, cada uno con su propia
 * transacción, para que ninguna conexión de base de datos quede retenida durante la llamada
 * HTTP externa y para que el estado de la operación sobreviva a un fallo del proveedor:
 * <ol>
 *   <li>Preparar: bloquea la cuenta, valida y registra la transacción como PENDIENTE.</li>
 *   <li>Enviar: llama a sendTx.do sin transacción abierta.</li>
 *   <li>Liquidar: solo con código 01 descuenta el saldo; en cualquier otro caso no se cobra.</li>
 * </ol>
 */
@Service
@Slf4j
public class PagoServicioService {

    private static final String ESTATUS_PENDIENTE = "PENDIENTE";
    private static final String ESTATUS_EXITOSA = "EXITOSA";
    private static final String ESTATUS_RECHAZADA = "RECHAZADA";
    private static final String ESTATUS_EN_REVISION = "EN_REVISION";
    private static final String CODIGO_EXITO = "01";
    private static final int MAX_LONGITUD_MENSAJE = 250;
    private static final int MAX_LONGITUD_CODIGO = 20;

    private final CuentaRepository cuentaRepository;
    private final TransaccionServicioRepository transaccionRepository;
    private final ProductoMongoRepository productoMongoRepository;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoTxClient txClient;
    private final AesEncryptionUtil aesUtil;
    private final PasswordEncoder passwordEncoder;
    private final JaxbXmlParser jaxbXmlParser;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer defaultDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String defaultCodigoDispositivo;

    @Value("${gestopago.aes.secret-key:}")
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
                               ObjectMapper objectMapper,
                               PlatformTransactionManager transactionManager) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.productoMongoRepository = productoMongoRepository;
        this.tokenService = tokenService;
        this.txClient = txClient;
        this.aesUtil = aesUtil;
        this.passwordEncoder = passwordEncoder;
        this.jaxbXmlParser = jaxbXmlParser;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PagoServicioResponseDto procesarPagoServicio(PagoServicioRequest request) {
        log.info("Iniciando pago de servicio: cuenta={}, idServicio={}, idProducto={}",
                enmascararCuenta(request.getNumCuenta()), request.getIdServicio(), request.getIdProducto());

        PagoPreparado preparado = transactionTemplate.execute(status -> prepararPago(request));
        ResultadoProveedor resultado = invocarProveedor(preparado);

        if (resultado == null) {
            registrarSinCobro(preparado.idTransaccion(), ESTATUS_EN_REVISION, null,
                    "Respuesta no concluyente del proveedor. Operación sujeta a conciliación.");
            throw new GestoPagoResultadoInciertoException(
                    "GestoPago devolvió una respuesta no concluyente. La transacción quedó EN_REVISION y no se descontó saldo.");
        }

        return transactionTemplate.execute(status -> liquidarPago(preparado, resultado));
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

        return transaccionRepository.findAll(spec, pageable)
                .map(tx -> construirRespuesta(tx, "Servicio #" + tx.getIdServicio(), "Producto #" + tx.getIdProducto()));
    }

    // ---------------------------------------------------------------------------------------
    // Paso 1: preparar (transacción corta con bloqueo pesimista de la cuenta)
    // ---------------------------------------------------------------------------------------

    private PagoPreparado prepararPago(PagoServicioRequest request) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaConBloqueo(request.getNumCuenta().trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta bancaria indicada no existe"));

        validarCuentaYAutorizacion(cuenta, request);

        String randomString = request.getRandomString().trim();
        if (transaccionRepository.existsByRandomString(randomString)) {
            throw new TransaccionDuplicadaException(
                    "Ya existe una transacción con el mismo folio de idempotencia (randomString)");
        }

        validarSaldoDisponible(cuenta, request.getPrecio());
        ProductoDocument producto = validarProductoYMonto(request);
        String bandera = validarBanderaYReferencia(request);

        String horaLocal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Integer distribuidor = request.getDistribuidor() != null ? request.getDistribuidor() : defaultDistribuidor;
        String jsonPayload = construirPayload(request, horaLocal, randomString);

        TransaccionServicio tx = transaccionRepository.save(TransaccionServicio.builder()
                .cuenta(cuenta)
                .randomString(randomString)
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .precio(request.getPrecio())
                .telefonoReferencia(request.getTelefonoReferencia().trim())
                .bandera(bandera)
                .distribuidor(distribuidor)
                .horaLocal(horaLocal)
                .payloadFirmadoAes(aesUtil.cifrar(jsonPayload, aesSecretKey))
                .estatus(ESTATUS_PENDIENTE)
                .build());

        return new PagoPreparado(tx.getId(), jsonPayload, producto.getServicio(), producto.getProducto());
    }

    private void validarCuentaYAutorizacion(Cuenta cuenta, PagoServicioRequest request) {
        if (!Boolean.TRUE.equals(cuenta.getActivo()) || !"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            log.warn("Rechazo de pago: cuenta {} inactiva o bloqueada", enmascararCuenta(cuenta.getNumeroCuenta()));
            throw new ValidacionNegocioException("La cuenta no se encuentra activa para operar.");
        }
        if (cuenta.getCliente() == null || !Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            log.warn("Rechazo de pago: cliente titular inactivo para cuenta {}", enmascararCuenta(cuenta.getNumeroCuenta()));
            throw new ValidacionNegocioException("El cliente titular de la cuenta se encuentra inactivo.");
        }
        Usuario usuario = cuenta.getCliente().getUsuario();
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(request.getPin().trim(), usuario.getPassword())) {
            log.warn("Fallo de autenticación al autorizar la transacción");
            throw new CredencialesInvalidasException("El PIN o contraseña de autorización es incorrecto.");
        }
    }

    /**
     * El saldo todavía no se descuenta al preparar, así que se restan los montos de operaciones
     * PENDIENTES o EN_REVISION para que dos pagos simultáneos no puedan comprometer el mismo dinero.
     */
    private void validarSaldoDisponible(Cuenta cuenta, BigDecimal monto) {
        BigDecimal comprometido = transaccionRepository.sumarMontosComprometidos(cuenta.getId());
        BigDecimal disponible = cuenta.getSaldo().subtract(comprometido != null ? comprometido : BigDecimal.ZERO);
        if (disponible.compareTo(monto) < 0) {
            log.warn("Rechazo por saldo insuficiente en cuenta {}", enmascararCuenta(cuenta.getNumeroCuenta()));
            throw new ValidacionNegocioException("Saldo disponible insuficiente para realizar la operación.");
        }
    }

    private ProductoDocument validarProductoYMonto(PagoServicioRequest request) {
        ProductoDocument producto = productoMongoRepository
                .findByIdProductoAndIdServicio(request.getIdProducto(), request.getIdServicio())
                .orElseThrow(() -> new ValidacionNegocioException("El producto " + request.getIdProducto()
                        + " no pertenece al servicio " + request.getIdServicio() + " o no existe en el catálogo."));

        if (producto.getPrecio() != null && producto.getPrecio() > 0.0) {
            BigDecimal precioCatalogo = BigDecimal.valueOf(producto.getPrecio());
            if (precioCatalogo.compareTo(request.getPrecio()) != 0) {
                log.warn("Monto no coincide con catálogo: solicitado={}, catálogo={}", request.getPrecio(), precioCatalogo);
                throw new ValidacionNegocioException("El monto solicitado no coincide con el precio del producto en el catálogo.");
            }
        }
        return producto;
    }

    private String validarBanderaYReferencia(PagoServicioRequest request) {
        String bandera = request.getBandera().trim().toUpperCase();
        String referencia = request.getTelefonoReferencia().trim();
        if ("SIN_REFERENCIA".equals(bandera) || "TIEMPO_AIRE".equals(bandera)) {
            if (!referencia.matches("\\d{10}")) {
                throw new ValidacionNegocioException(
                        "Para operaciones sin referencia (tiempo aire) el teléfono debe tener exactamente 10 dígitos.");
            }
        } else if ("CON_REFERENCIA".equals(bandera) || "PAGO_SERVICIO".equals(bandera)) {
            if (referencia.length() < 3) {
                throw new ValidacionNegocioException("La referencia del servicio debe tener al menos 3 caracteres.");
            }
        } else {
            throw new ValidacionNegocioException(
                    "Bandera no válida. Valores permitidos: SIN_REFERENCIA, TIEMPO_AIRE, CON_REFERENCIA, PAGO_SERVICIO.");
        }
        return bandera;
    }

    private String construirPayload(PagoServicioRequest request, String horaLocal, String randomString) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idProducto", request.getIdProducto());
        payload.put("idServicio", request.getIdServicio());
        payload.put("telefonoReferencia", request.getTelefonoReferencia().trim());
        payload.put("horaLocal", horaLocal);
        payload.put("montoPago", request.getPrecio());
        payload.put("upc", String.valueOf(request.getIdProducto()));
        payload.put("unidad", 1);
        payload.put("randomString", randomString);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar el payload de la transacción");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Paso 2: enviar a GestoPago (sin transacción abierta)
    // ---------------------------------------------------------------------------------------

    /**
     * Devuelve el resultado interpretado, o null si el proveedor respondió pero no se puede
     * determinar el resultado. Un timeout o un error 5xx nunca se toman como "no se procesó".
     */
    private ResultadoProveedor invocarProveedor(PagoPreparado preparado) {
        String bearer;
        try {
            bearer = "Bearer " + resolverToken();
        } catch (GestoPagoIntegrationException e) {
            registrarSinCobro(preparado.idTransaccion(), ESTATUS_RECHAZADA, null,
                    "No fue posible autenticarse con el proveedor; la operación no se envió.");
            throw e;
        }

        try {
            return interpretar(txClient.enviarTransaccionJson(bearer, preparado.jsonPayload()));
        } catch (RetryableException e) {
            log.error("Timeout de red en sendTx.do; la transacción {} queda EN_REVISION", preparado.idTransaccion());
            registrarSinCobro(preparado.idTransaccion(), ESTATUS_EN_REVISION, null,
                    "Timeout de comunicación con el proveedor. Operación sujeta a conciliación.");
            throw new GestoPagoResultadoInciertoException(
                    "Tiempo de espera agotado con GestoPago. La transacción quedó EN_REVISION y no se descontó saldo.");
        } catch (FeignException e) {
            log.warn("GestoPago respondió HTTP {} para la transacción {}", e.status(), preparado.idTransaccion());
            ResultadoProveedor interpretado = interpretar(e.contentUTF8());
            if (interpretado != null) {
                return interpretado;
            }
            if (e.status() >= 400 && e.status() < 500) {
                return new ResultadoProveedor(String.valueOf(e.status()), "El proveedor rechazó la solicitud", null, null);
            }
            registrarSinCobro(preparado.idTransaccion(), ESTATUS_EN_REVISION, String.valueOf(e.status()),
                    "Error del servidor del proveedor. Operación sujeta a conciliación.");
            throw new GestoPagoResultadoInciertoException(
                    "GestoPago respondió con un error de servidor. La transacción quedó EN_REVISION y no se descontó saldo.");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Paso 3: liquidar (solo el código 01 descuenta saldo)
    // ---------------------------------------------------------------------------------------

    private PagoServicioResponseDto liquidarPago(PagoPreparado preparado, ResultadoProveedor resultado) {
        if (!CODIGO_EXITO.equals(resultado.codigo())) {
            log.warn("Transacción {} no exitosa en GestoPago (código={}). No se descuenta saldo.",
                    preparado.idTransaccion(), resultado.codigo());
            TransaccionServicio tx = cargarTransaccion(preparado.idTransaccion());
            aplicarResultado(tx, ESTATUS_RECHAZADA, resultado);
            return respuesta(transaccionRepository.save(tx), preparado);
        }

        TransaccionServicio original = cargarTransaccion(preparado.idTransaccion());
        int filas = cuentaRepository.descontarSaldoSiSuficiente(original.getCuenta().getNumeroCuenta(), original.getPrecio());

        // El UPDATE limpia el contexto de persistencia: se vuelve a leer para trabajar con el saldo real.
        TransaccionServicio tx = cargarTransaccion(preparado.idTransaccion());
        if (filas == 0) {
            log.error("GestoPago confirmó (01) la transacción {} pero el saldo ya no alcanzó. Requiere conciliación manual.",
                    preparado.idTransaccion());
            aplicarResultado(tx, ESTATUS_EN_REVISION, resultado);
            tx.setMensajeProveedor("Confirmada por el proveedor pero sin saldo para asentar el cargo. Requiere conciliación.");
        } else {
            aplicarResultado(tx, ESTATUS_EXITOSA, resultado);
        }
        return respuesta(transaccionRepository.save(tx), preparado);
    }

    private void aplicarResultado(TransaccionServicio tx, String estatus, ResultadoProveedor resultado) {
        tx.setEstatus(estatus);
        tx.setCodigoRespuestaProveedor(abreviar(resultado.codigo(), MAX_LONGITUD_CODIGO));
        tx.setMensajeProveedor(abreviar(resultado.texto(), MAX_LONGITUD_MENSAJE));
        tx.setFolioProveedor(resultado.folio());
        tx.setAutorizacionProveedor(resultado.autorizacion());
    }

    private void registrarSinCobro(Long idTransaccion, String estatus, String codigo, String mensaje) {
        transactionTemplate.executeWithoutResult(status -> {
            TransaccionServicio tx = cargarTransaccion(idTransaccion);
            tx.setEstatus(estatus);
            tx.setCodigoRespuestaProveedor(codigo);
            tx.setMensajeProveedor(mensaje);
            transaccionRepository.save(tx);
        });
    }

    private TransaccionServicio cargarTransaccion(Long id) {
        return transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("No se encontró la transacción " + id));
    }

    private PagoServicioResponseDto respuesta(TransaccionServicio tx, PagoPreparado preparado) {
        return construirRespuesta(tx, preparado.nombreServicio(), preparado.nombreProducto());
    }

    private PagoServicioResponseDto construirRespuesta(TransaccionServicio tx, String nombreServicio, String nombreProducto) {
        return PagoServicioResponseDto.builder()
                .idTransaccion(tx.getId())
                .randomString(tx.getRandomString())
                .numCuenta(enmascararCuenta(tx.getCuenta().getNumeroCuenta()))
                .idServicio(tx.getIdServicio())
                .idProducto(tx.getIdProducto())
                .nombreServicio(nombreServicio)
                .nombreProducto(nombreProducto)
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

    // ---------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------

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

    /** Interpreta la respuesta XML o JSON del proveedor una sola vez. Devuelve null si no es concluyente. */
    private ResultadoProveedor interpretar(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String contenido = raw.trim();
        try {
            if (contenido.startsWith("<")) {
                TxXmlResponse xml = jaxbXmlParser.unmarshal(contenido, TxXmlResponse.class);
                if (xml.getMensaje() == null || xml.getMensaje().getCodigo() == null) {
                    return null;
                }
                TxDetalleXml detalle = xml.getTransaccion();
                return new ResultadoProveedor(xml.getMensaje().getCodigo().trim(), xml.getMensaje().getTexto(),
                        detalle != null ? detalle.getFolio() : null,
                        detalle != null ? detalle.getAutorizacion() : null);
            }
            if (contenido.startsWith("{")) {
                JsonNode root = objectMapper.readTree(contenido);
                String codigo = valorDe(root, "codigo", "CODIGO");
                if (codigo == null) {
                    return null;
                }
                return new ResultadoProveedor(codigo.trim(), valorDe(root, "mensaje", "texto", "TEXTO"),
                        valorDe(root, "folio", "FOLIO"), valorDe(root, "autorizacion", "AUTORIZACION"));
            }
        } catch (Exception e) {
            log.warn("La respuesta del proveedor no pudo interpretarse");
        }
        return null;
    }

    private String valorDe(JsonNode root, String... nombres) {
        for (String nombre : nombres) {
            if (root.hasNonNull(nombre)) {
                return root.get(nombre).asText();
            }
        }
        return null;
    }

    private String abreviar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private String enmascararCuenta(String cuenta) {
        if (cuenta == null || cuenta.length() < 8) {
            return "****";
        }
        return cuenta.substring(0, 4) + "********" + cuenta.substring(cuenta.length() - 4);
    }

    private String enmascararReferencia(String referencia) {
        if (referencia == null || referencia.length() < 4) {
            return "****";
        }
        if (referencia.length() == 10) {
            return referencia.substring(0, 2) + "******" + referencia.substring(8);
        }
        return referencia.substring(0, 2) + "****" + referencia.substring(referencia.length() - 2);
    }

    private record PagoPreparado(Long idTransaccion, String jsonPayload, String nombreServicio, String nombreProducto) {
    }

    private record ResultadoProveedor(String codigo, String texto, String folio, String autorizacion) {
    }
}
