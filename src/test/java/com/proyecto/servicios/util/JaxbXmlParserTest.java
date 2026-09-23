package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoXmlResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JaxbXmlParserTest {

    private JaxbXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new JaxbXmlParser();
    }

    @Test
    @DisplayName("Debe deserializar correctamente un XML válido de catálogo de GestoPago")
    void testUnmarshal_Exitoso() {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>01</CODIGO>\n" +
                "        <TEXTO>Operacion realizada con exito</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "    <PRODUCTOS>\n" +
                "        <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200' " +
                "                  idCatTipoServicio='10' tipoFront='1' hasDigitoVerificador='false' precio='100.0' " +
                "                  showAyuda='false' tipoReferencia='a'>\n" +
                "            <legend><![CDATA[Instrucciones de redencion Amazon]]></legend>\n" +
                "        </producto>\n" +
                "        <producto servicio='CFE' producto='CFE Pago' idServicio='50' idProducto='301' " +
                "                  idCatTipoServicio='5' tipoFront='2' hasDigitoVerificador='true' precio='10.0' " +
                "                  showAyuda='false' tipoReferencia='b'>\n" +
                "            <legend>Conserva tu recibo</legend>\n" +
                "        </producto>\n" +
                "    </PRODUCTOS>\n" +
                "</RESPONSE>";

        CatalogoXmlResponse response = parser.unmarshal(xml, CatalogoXmlResponse.class);

        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());

        assertNotNull(response.getProductos());
        assertEquals(2, response.getProductos().size());

        ProductoXml p1 = response.getProductos().get(0);
        assertEquals("Amazon", p1.getServicio());
        assertEquals("Amazon $100", p1.getProducto());
        assertEquals(71, p1.getIdServicio());
        assertEquals(200, p1.getIdProducto());
        assertEquals(1, p1.getTipoFront());
        assertEquals(100.0, p1.getPrecio());
        assertEquals("Instrucciones de redencion Amazon", p1.getLegend());

        ProductoXml p2 = response.getProductos().get(1);
        assertEquals("CFE", p2.getServicio());
        assertEquals(50, p2.getIdServicio());
        assertEquals(301, p2.getIdProducto());
        assertEquals(2, p2.getTipoFront());
        assertTrue(p2.getHasDigitoVerificador());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el XML está vacío o es nulo")
    void testUnmarshal_XmlVacio_LanzaExcepcion() {
        assertThrows(GestoPagoIntegrationException.class, () -> parser.unmarshal("", CatalogoXmlResponse.class));
        assertThrows(GestoPagoIntegrationException.class, () -> parser.unmarshal(null, CatalogoXmlResponse.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el XML está mal formado")
    void testUnmarshal_XmlInvalido_LanzaExcepcion() {
        String xmlMalformado = "<RESPONSE><MENSAJE><CODIGO>01</RESPONSE>";
        assertThrows(GestoPagoIntegrationException.class, () -> parser.unmarshal(xmlMalformado, CatalogoXmlResponse.class));
    }
}
