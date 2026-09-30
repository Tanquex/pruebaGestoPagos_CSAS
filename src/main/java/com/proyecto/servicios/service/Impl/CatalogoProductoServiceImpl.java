package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoXmlResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoXml;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.service.CatalogoProductoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoProductoServiceImpl implements CatalogoProductoService {

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

    public CatalogoProductoServiceImpl(GestoPagoCatalogoClient catalogoClient,
                                       ProductoMongoRepository productoMongoRepository,
                                       GestoPagoTokenService tokenService,
                                       JaxbXmlParser jaxbXmlParser) {
        this.catalogoClient = catalogoClient;
        this.productoMongoRepository = productoMongoRepository;
        this.tokenService = tokenService;
        this.jaxbXmlParser = jaxbXmlParser;
    }

    /**
     * Tarea programada para sincronizar el catálogo diariamente a las 06:00 AM
     * o según la expresión configurada en gestopago.catalogo.cron.
     */
    @Override
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
                log.warn("El proveedor reportó un código no exitoso en el catálogo: {}", codigo);
            }
        }

        List<ProductoXml> productosXml = response.getProductos() != null ? response.getProductos() : new ArrayList<>();
        log.info("Se recibieron {} productos desde GestoPago. Procediendo a guardar en MongoDB...", productosXml.size());

        LocalDateTime ahora = LocalDateTime.now();
        List<ProductoDocument> documentos = productosXml.stream()
                .map(pxml -> {
                    // Buscar si ya existe en MongoDB por idProducto para conservar el mismo ID de documento
                    String existingId = productoMongoRepository.findByIdProducto(pxml.getIdProducto())
                            .map(ProductoDocument::getId)
                            .orElse(null);

                    return ProductoDocument.builder()
                            .id(existingId)
                            .idProducto(pxml.getIdProducto())
                            .idServicio(pxml.getIdServicio())
                            .servicio(pxml.getServicio())
                            .producto(pxml.getProducto())
                            .idCatTipoServicio(pxml.getIdCatTipoServicio())
                            .tipoFront(pxml.getTipoFront())
                            .hasDigitoVerificador(pxml.getHasDigitoVerificador())
                            .precio(pxml.getPrecio())
                            .showAyuda(pxml.getShowAyuda())
                            .tipoReferencia(pxml.getTipoReferencia())
                            .legend(pxml.getLegend())
                            .fechaSincronizacion(ahora)
                            .build();
                })
                .toList();

        List<ProductoDocument> guardados = productoMongoRepository.saveAll(documentos);
        long duracion = System.currentTimeMillis() - startTime;
        log.info("Sincronización finalizada exitosamente: {} productos actualizados en MongoDB en {} ms", guardados.size(), duracion);

        return guardados;
    }

    @Override
    public List<ProductoDocument> obtenerTodos() {
        return productoMongoRepository.findAll();
    }

    @Override
    public List<ProductoDocument> obtenerPorServicio(Integer idServicio) {
        return productoMongoRepository.findByIdServicio(idServicio);
    }

    @Override
    public List<ProductoDocument> buscarPorNombre(String nombre) {
        return productoMongoRepository.findByServicioContainingIgnoreCase(nombre);
    }

    @Override
    public Optional<ProductoDocument> obtenerPorIdProducto(Integer idProducto) {
        return productoMongoRepository.findByIdProducto(idProducto);
    }

    /**
     * Resuelve el token activo buscando primero en la base de datos PostgreSQL,
     * o en caso de no existir, usa el token de configuración de respaldo.
     */
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
