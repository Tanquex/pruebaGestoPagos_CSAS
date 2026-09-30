package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoXmlResponse;
import com.proyecto.servicios.model.gestopago.catalogo.MensajeXml;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoXml;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.service.Impl.CatalogoProductoServiceImpl;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoProductoServiceImplTest {

    @Mock
    private GestoPagoCatalogoClient catalogoClient;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private JaxbXmlParser jaxbXmlParser;

    @InjectMocks
    private CatalogoProductoServiceImpl catalogoProductoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(catalogoProductoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(catalogoProductoService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(catalogoProductoService, "fallbackToken", "token-de-prueba");
    }

    @Test
    @DisplayName("Sincronización exitosa: Obtiene token, consume Feign, parsea con JAXB y guarda en MongoDB")
    void testSincronizarCatalogo_Exitoso() {
        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("token-jwt-activo");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));

        String mockXml = "<RESPONSE>...</RESPONSE>";
        when(catalogoClient.getProductListXml("Bearer token-jwt-activo")).thenReturn(mockXml);

        ProductoXml pXml = ProductoXml.builder()
                .idProducto(200)
                .idServicio(71)
                .servicio("Amazon")
                .producto("Amazon $100")
                .precio(100.0)
                .tipoFront(1)
                .build();

        CatalogoXmlResponse responseXml = CatalogoXmlResponse.builder()
                .mensaje(new MensajeXml("01", "Operacion realizada con exito"))
                .productos(List.of(pXml))
                .build();

        when(jaxbXmlParser.unmarshal(eq(mockXml), eq(CatalogoXmlResponse.class))).thenReturn(responseXml);
        when(productoMongoRepository.findByIdProducto(200)).thenReturn(Optional.empty());

        ProductoDocument docGuardado = ProductoDocument.builder()
                .id("mongo-id-1")
                .idProducto(200)
                .idServicio(71)
                .servicio("Amazon")
                .producto("Amazon $100")
                .build();

        when(productoMongoRepository.saveAll(anyList())).thenReturn(List.of(docGuardado));

        List<ProductoDocument> resultado = catalogoProductoService.sincronizarCatalogo();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Amazon", resultado.get(0).getServicio());

        verify(catalogoClient, times(1)).getProductListXml("Bearer token-jwt-activo");
        verify(jaxbXmlParser, times(1)).unmarshal(mockXml, CatalogoXmlResponse.class);
        verify(productoMongoRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Sincronización fallida: Lanza excepción cuando ocurre timeout en el cliente Feign")
    void testSincronizarCatalogo_Timeout_LanzaExcepcion() {
        when(tokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, StandardCharsets.UTF_8, new RequestTemplate());

        RetryableException timeoutException = new RetryableException(
                504, "Read timed out", Request.HttpMethod.GET, new Date(), request);

        when(catalogoClient.getProductListXml(anyString())).thenThrow(timeoutException);

        GestoPagoIntegrationException ex = assertThrows(GestoPagoIntegrationException.class, () ->
                catalogoProductoService.sincronizarCatalogo());

        assertTrue(ex.getMessage().contains("Timeout"));
        verify(productoMongoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Sincronización fallida: Lanza excepción si no se encuentra ningún token activo")
    void testSincronizarCatalogo_SinToken_LanzaExcepcion() {
        ReflectionTestUtils.setField(catalogoProductoService, "fallbackToken", null);
        when(tokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());

        GestoPagoIntegrationException ex = assertThrows(GestoPagoIntegrationException.class, () ->
                catalogoProductoService.sincronizarCatalogo());

        assertTrue(ex.getMessage().contains("No se encontró un token activo"));
        verify(catalogoClient, never()).getProductListXml(anyString());
    }

    @Test
    @DisplayName("Consulta por servicio: Retorna la lista de productos filtrada desde MongoDB")
    void testObtenerPorServicio() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(200)
                .idServicio(71)
                .servicio("Amazon")
                .build();

        when(productoMongoRepository.findByIdServicio(71)).thenReturn(List.of(doc));

        List<ProductoDocument> docs = catalogoProductoService.obtenerPorServicio(71);

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals(71, docs.get(0).getIdServicio());
        verify(productoMongoRepository, times(1)).findByIdServicio(71);
    }

    @Test
    @DisplayName("Búsqueda por nombre: Retorna coincidencias desde MongoDB")
    void testBuscarPorNombre() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(185)
                .idServicio(56)
                .servicio("AGUAKAN")
                .build();

        when(productoMongoRepository.findByServicioContainingIgnoreCase("AGUA")).thenReturn(List.of(doc));

        List<ProductoDocument> docs = catalogoProductoService.buscarPorNombre("AGUA");

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals("AGUAKAN", docs.get(0).getServicio());
        verify(productoMongoRepository, times(1)).findByServicioContainingIgnoreCase("AGUA");
    }
}
