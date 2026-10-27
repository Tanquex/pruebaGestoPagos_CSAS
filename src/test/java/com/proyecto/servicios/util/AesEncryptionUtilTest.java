package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    private AesEncryptionUtil aesUtil;
    private final String secretKey = "MiClaveSecretaGP";

    @BeforeEach
    void setUp() {
        aesUtil = new AesEncryptionUtil();
    }

    @Test
    @DisplayName("Debe cifrar y descifrar un texto correctamente con AES")
    void testCifrarYDescifrar() {
        String textoOriginal = "{\"idProducto\":200,\"monto\":100.00,\"telefono\":\"5512345678\"}";

        String cifradoBase64 = aesUtil.cifrar(textoOriginal, secretKey);
        assertNotNull(cifradoBase64);
        assertFalse(cifradoBase64.isBlank());
        assertNotEquals(textoOriginal, cifradoBase64);

        String descifrado = aesUtil.descifrar(cifradoBase64, secretKey);
        assertEquals(textoOriginal, descifrado);
    }

    @Test
    @DisplayName("Debe manejar cadenas vacías o nulas sin lanzar excepciones no controladas")
    void testManejoCadenasVacias() {
        assertEquals("", aesUtil.cifrar("", secretKey));
        assertEquals("", aesUtil.cifrar(null, secretKey));
        assertEquals("", aesUtil.descifrar("", secretKey));
        assertEquals("", aesUtil.descifrar(null, secretKey));
    }
}
