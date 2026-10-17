package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.catalogo.ProductoConsultaRequest;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoXmlResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoXml;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoProductoService {

    private final GestoPagoCatalogoClient catalogoClient;
    private final ProductoMongoRepository productoMongoRepository;
    private final GestoPagoTokenService tokenService;
    private final JaxbXmlParser jaxbXmlParser;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.token:}")
    private String fallbackToken;

    public CatalogoProductoService(GestoPagoCatalogoClient catalogoClient,
                                  ProductoMongoRepository productoMongoRepository,
                                  GestoPagoTokenService tokenService,
                                  JaxbXmlParser jaxbXmlParser) {
        this.catalogoClient = catalogoClient;
        this.productoMongoRepository = productoMongoRepository;
        this.tokenService = tokenService;
        this.jaxbXmlParser = jaxbXmlParser;
    }

    @Scheduled(cron = "${gestopago.catalogo.cron:0 0 6 * * *}")
    public List<ProductoDocument> sincronizarCatalogo() {
        long startTime = System.currentTimeMillis();
        log.info("Iniciando sincronización del catálogo de productos con GestoPago...");

        String token = resolverToken();
        String bearerHeader = "Bearer " + token;

        String rawXml;
        try {
            rawXml = catalogoClient.getProductListXml(bearerHeader);
        } catch (RetryableException e) {
            log.error("Timeout o error de conexión al consultar catálogo de GestoPago: {}", e.getMessage());
            throw new GestoPagoIntegrationException("Timeout de comunicación al consultar el catálogo de GestoPago", e);
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("Error de autenticación con GestoPago (código HTTP {})", e.status());
            throw new GestoPagoIntegrationException("Credenciales o token no válidos para autenticación con el proveedor", e.status());
        } catch (FeignException e) {
            log.error("Error en llamada al proveedor GestoPago con código HTTP {}", e.status());
            throw new GestoPagoIntegrationException("Respuesta no exitosa del proveedor GestoPago", e.status());
        } catch (Exception e) {
            log.error("Error inesperado en comunicación con GestoPago: {}", e.getMessage());
            throw new GestoPagoIntegrationException("Fallo en la comunicación con el servicio de GestoPago", e);
        }

        CatalogoXmlResponse response = jaxbXmlParser.unmarshal(rawXml, CatalogoXmlResponse.class);

        if (response.getMensaje() != null) {
            String codigo = response.getMensaje().getCodigo();
            String texto = response.getMensaje().getTexto();
            log.info("Respuesta del proveedor GestoPago: CODIGO='{}', TEXTO='{}'", codigo, texto);

            if (codigo != null && !codigo.equals("01") && !codigo.equals("1")) {
                throw new GestoPagoIntegrationException("Error en catálogo del proveedor GestoPago: " + texto);
            }
        }

        List<ProductoXml> productosXml = response.getProductos();
        if (productosXml == null || productosXml.isEmpty()) {
            log.warn("El catálogo devuelto por GestoPago no contiene productos");
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<ProductoDocument> documentos = productosXml.stream()
                .map(px -> {
                    Optional<ProductoDocument> existente = productoMongoRepository.findByIdProducto(px.getIdProducto());
                    return ProductoDocument.builder()
                            .id(existente.map(ProductoDocument::getId).orElse(null))
                            .idProducto(px.getIdProducto())
                            .idServicio(px.getIdServicio())
                            .servicio(px.getServicio())
                            .producto(px.getProducto())
                            .idCatTipoServicio(px.getIdCatTipoServicio())
                            .tipoFront(px.getTipoFront())
                            .hasDigitoVerificador(px.getHasDigitoVerificador())
                            .precio(px.getPrecio())
                            .showAyuda(px.getShowAyuda())
                            .tipoReferencia(px.getTipoReferencia())
                            .legend(px.getLegend())
                            .fechaSincronizacion(now)
                            .build();
                })
                .toList();

        List<ProductoDocument> guardados = productoMongoRepository.saveAll(documentos);
        long duracion = System.currentTimeMillis() - startTime;
        log.info("Sincronización finalizada exitosamente: {} productos actualizados en MongoDB en {} ms", guardados.size(), duracion);

        return guardados;
    }

    public List<ProductoDocument> consultarProductos(ProductoConsultaRequest request) {
        if (request != null && request.getIdProducto() != null) {
            return productoMongoRepository.findByIdProducto(request.getIdProducto())
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (request != null && request.getIdServicio() != null) {
            return productoMongoRepository.findByIdServicio(request.getIdServicio());
        }
        if (request != null && request.getBuscar() != null && !request.getBuscar().isBlank()) {
            return productoMongoRepository.findByServicioContainingIgnoreCase(request.getBuscar().trim());
        }
        return productoMongoRepository.findAll();
    }

    public List<ProductoDocument> obtenerTodos() {
        return productoMongoRepository.findAll();
    }

    public List<ProductoDocument> obtenerPorServicio(Integer idServicio) {
        return productoMongoRepository.findByIdServicio(idServicio);
    }

    public List<ProductoDocument> buscarPorNombre(String nombre) {
        return productoMongoRepository.findByServicioContainingIgnoreCase(nombre);
    }

    public Optional<ProductoDocument> obtenerPorIdProducto(Integer idProducto) {
        return productoMongoRepository.findByIdProducto(idProducto);
    }

    private String resolverToken() {
        Optional<GestoPagoToken> tokenDb = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (tokenDb.isPresent() && tokenDb.get().getToken() != null && !tokenDb.get().getToken().isBlank()) {
            return tokenDb.get().getToken();
        }

        if (fallbackToken != null && !fallbackToken.isBlank()) {
            return fallbackToken;
        }

        throw new GestoPagoIntegrationException("No se encontró un token activo en base de datos ni en la configuración para el distribuidor " + idDistribuidor);
    }
}
