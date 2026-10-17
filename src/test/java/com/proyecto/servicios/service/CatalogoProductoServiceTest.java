package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.catalogo.ProductoConsultaRequest;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoXmlResponse;
import com.proyecto.servicios.model.gestopago.catalogo.MensajeXml;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoXml;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
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
class CatalogoProductoServiceTest {

    @Mock
    private GestoPagoCatalogoClient catalogoClient;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private JaxbXmlParser jaxbXmlParser;

    @InjectMocks
    private CatalogoProductoService catalogoProductoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(catalogoProductoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(catalogoProductoService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(catalogoProductoService, "fallbackToken", "token-de-prueba");
    }

    @Test
    @DisplayName("Sincronización exitosa con respuesta XML correcta")
    void testSincronizarCatalogo_Exitoso() {
        String xmlPrueba = "<CatalogoXmlResponse><Mensaje><Codigo>01</Codigo><Texto>Exito</Texto></Mensaje></CatalogoXmlResponse>";

        GestoPagoToken tokenDb = new GestoPagoToken();
        tokenDb.setToken("token-valido-db");
        tokenDb.setActivo(true);
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenDb));
        when(catalogoClient.getProductListXml("Bearer token-valido-db")).thenReturn(xmlPrueba);

        ProductoXml p1 = new ProductoXml();
        p1.setIdProducto(1);
        p1.setIdServicio(10);
        p1.setServicio("Luz CFE");

        CatalogoXmlResponse responseMock = new CatalogoXmlResponse();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("OK");
        responseMock.setMensaje(mensaje);
        responseMock.setProductos(List.of(p1));

        when(jaxbXmlParser.unmarshal(eq(xmlPrueba), eq(CatalogoXmlResponse.class))).thenReturn(responseMock);
        when(productoMongoRepository.findByIdProducto(1)).thenReturn(Optional.empty());
        when(productoMongoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProductoDocument> resultado = catalogoProductoService.sincronizarCatalogo();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Luz CFE", resultado.get(0).getServicio());
        verify(productoMongoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Consultar catálogo por Request DTO dinámico")
    void testConsultarProductos_PorServicio() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(101)
                .idServicio(5)
                .servicio("Agua Potable")
                .build();

        when(productoMongoRepository.findByIdServicio(5)).thenReturn(List.of(doc));

        ProductoConsultaRequest req = ProductoConsultaRequest.builder().idServicio(5).build();
        List<ProductoDocument> result = catalogoProductoService.consultarProductos(req);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Agua Potable", result.get(0).getServicio());
    }

    @Test
    @DisplayName("Manejo de timeout al comunicarse con GestoPago lanza GestoPagoIntegrationException")
    void testSincronizarCatalogo_Timeout() {
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());

        Request request = Request.create(Request.HttpMethod.GET, "/url", Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(catalogoClient.getProductListXml(anyString()))
                .thenThrow(new RetryableException(504, "Gateway Timeout", Request.HttpMethod.GET, new Date(), request));

        assertThrows(GestoPagoIntegrationException.class, () ->
                catalogoProductoService.sincronizarCatalogo());
    }
}
