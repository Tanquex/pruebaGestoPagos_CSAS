package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ValidacionNegocioException;
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

    public String cifrar(String textoPlano, String claveSecreta) {
        if (textoPlano == null || textoPlano.isBlank()) {
            return "";
        }
        try {
            SecretKeySpec secretKey = generarSecretKey(claveSecreta);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] bytesCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytesCifrados);
        } catch (Exception e) {
            log.error("Error al cifrar información con AES: {}", e.getMessage());
            throw new ValidacionNegocioException("Error interno al firmar la transacción mediante AES", e);
        }
    }

    public String descifrar(String textoCifrado, String claveSecreta) {
        if (textoCifrado == null || textoCifrado.isBlank()) {
            return "";
        }
        try {
            SecretKeySpec secretKey = generarSecretKey(claveSecreta);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytesOriginales = cipher.doFinal(Base64.getDecoder().decode(textoCifrado));
            return new String(bytesOriginales, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar información con AES: {}", e.getMessage());
            throw new ValidacionNegocioException("Error interno al descifrar la transacción mediante AES", e);
        }
    }

    private SecretKeySpec generarSecretKey(String clave) {
        if (clave == null || clave.isBlank()) {
            clave = "GestoPagoAES2026";
        }
        byte[] keyBytes = Arrays.copyOf(clave.getBytes(StandardCharsets.UTF_8), 16);
        return new SecretKeySpec(keyBytes, ALGORITMO);
    }
}
