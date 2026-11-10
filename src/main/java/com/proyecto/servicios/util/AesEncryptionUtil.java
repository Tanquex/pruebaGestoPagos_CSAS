package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
@Slf4j
public class AesEncryptionUtil {

    private static final String ALGORITMO = "AES";
    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int LONGITUD_CLAVE_BYTES = 16;
    private static final int LONGITUD_IV_BYTES = 12;
    private static final int LONGITUD_TAG_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();

    public String cifrar(String textoPlano, String claveSecreta) {
        if (textoPlano == null || textoPlano.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(LONGITUD_TAG_BITS, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, generarSecretKey(claveSecreta), spec);
            byte[] bytesCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + bytesCifrados.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(bytesCifrados, 0, resultado, iv.length, bytesCifrados.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cifrar información con AES", e);
            throw new IllegalStateException("Error interno al firmar la transacción mediante AES", e);
        }
    }

    public String descifrar(String textoCifrado, String claveSecreta) {
        if (textoCifrado == null || textoCifrado.isBlank()) {
            return "";
        }
        try {
            byte[] bytesTotales = Base64.getDecoder().decode(textoCifrado);
            if (bytesTotales.length < LONGITUD_IV_BYTES) {
                throw new IllegalArgumentException("Payload cifrado inválido: longitud insuficiente para IV");
            }

            byte[] iv = Arrays.copyOfRange(bytesTotales, 0, LONGITUD_IV_BYTES);
            byte[] bytesCifrados = Arrays.copyOfRange(bytesTotales, LONGITUD_IV_BYTES, bytesTotales.length);
            GCMParameterSpec spec = new GCMParameterSpec(LONGITUD_TAG_BITS, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, generarSecretKey(claveSecreta), spec);
            byte[] bytesOriginales = cipher.doFinal(bytesCifrados);
            return new String(bytesOriginales, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al descifrar información con AES", e);
            throw new IllegalStateException("Error interno al descifrar la transacción mediante AES", e);
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
