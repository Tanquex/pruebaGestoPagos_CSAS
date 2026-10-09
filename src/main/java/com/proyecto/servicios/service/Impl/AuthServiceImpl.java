package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String SECRET = "GestoPagoSuperSecretKeyForJWTTokenSigning2026!";
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de autenticación para el usuario: {}", correo);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> {
                    log.warn("Autenticación rechazada: Usuario no encontrado para correo: {}", correo);
                    return new CredencialesInvalidasException("Credenciales inválidas o usuario inactivo.");
                });

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Autenticación rechazada: Usuario inactivo para correo: {}", correo);
            throw new CredencialesInvalidasException("Credenciales inválidas o usuario inactivo.");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Autenticación rechazada: Contraseña incorrecta para correo: {}", correo);
            throw new CredencialesInvalidasException("Credenciales inválidas o usuario inactivo.");
        }

        Long clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        String token = generarJwtToken(usuario.getCorreo(), clienteId);

        log.info("Autenticación exitosa para usuario: {}, clienteId: {}", correo, clienteId);
        return LoginResponse.builder()
                .token(token)
                .tipoToken("Bearer")
                .correo(usuario.getCorreo())
                .clienteId(clienteId)
                .mensaje("Autenticación exitosa")
                .build();
    }

    private String generarJwtToken(String correo, Long clienteId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (24 * 60 * 60); // 24 horas de vigencia

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = String.format("{\"sub\":\"%s\",\"clienteId\":%s,\"iat\":%d,\"exp\":%d}",
                    correo, clienteId != null ? clienteId.toString() : "null", now, exp);

            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String encodedHeader = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = encodedHeader + "." + encodedPayload;

            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);

            byte[] signatureBytes = hmacSha256.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = encoder.encodeToString(signatureBytes);

            return dataToSign + "." + encodedSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error al generar token JWT", e);
            throw new RuntimeException("Error en generación de token de seguridad", e);
        }
    }
}
