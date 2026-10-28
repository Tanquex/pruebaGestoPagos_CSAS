package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Component
@Slf4j
public class AesEncryptionUtil {

    private static final String ALGORITMO = "AES";
    private static final String TRANSFORMACION = "AES/ECB/PKCS5Padding";
    private static final int LONGITUD_CLAVE_BYTES = 16;

    public String cifrar(String textoPlano, String claveSecreta) {
        if (textoPlano == null || textoPlano.isBlank()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, generarSecretKey(claveSecreta));
            byte[] bytesCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytesCifrados);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cifrar información con AES");
            throw new IllegalStateException("Error interno al firmar la transacción mediante AES");
        }
    }

    public String descifrar(String textoCifrado, String claveSecreta) {
        if (textoCifrado == null || textoCifrado.isBlank()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, generarSecretKey(claveSecreta));
            byte[] bytesOriginales = cipher.doFinal(Base64.getDecoder().decode(textoCifrado));
            return new String(bytesOriginales, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al descifrar información con AES");
            throw new IllegalStateException("Error interno al descifrar la transacción mediante AES");
        }
    }

    private SecretKeySpec generarSecretKey(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("La clave AES no está configurada (variable de entorno GESTOPAGO_AES_KEY)");
        }
        byte[] keyBytes = Arrays.copyOf(clave.getBytes(StandardCharsets.UTF_8), LONGITUD_CLAVE_BYTES);
        return new SecretKeySpec(keyBytes, ALGORITMO);
    }
}
