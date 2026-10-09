package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Login exitoso devuelve token JWT y datos de sesión")
    void testLogin_Exitoso() {
        Cliente cliente = Cliente.builder().id(5L).build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .cliente(cliente)
                .correo("juan@test.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .build();

        LoginRequest req = LoginRequest.builder()
                .correo("juan@test.com")
                .password("Password123")
                .build();

        when(usuarioRepository.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hashedPassword")).thenReturn(true);

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertTrue(resp.getToken().split("\\.").length == 3, "El token debe tener 3 segmentos estándar JWT (header.payload.signature)");
        assertEquals("Bearer", resp.getTipoToken());
        assertEquals("juan@test.com", resp.getCorreo());
        assertEquals(5L, resp.getClienteId());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta lanza CredencialesInvalidasException")
    void testLogin_PasswordIncorrecto_LanzaExcepcion() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .correo("juan@test.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .build();

        LoginRequest req = LoginRequest.builder()
                .correo("juan@test.com")
                .password("PasswordErroneo")
                .build();

        when(usuarioRepository.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PasswordErroneo", "$2a$10$hashedPassword")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () ->
                authService.login(req));
    }

    @Test
    @DisplayName("Login con usuario inactivo lanza CredencialesInvalidasException")
    void testLogin_UsuarioInactivo_LanzaExcepcion() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .correo("juan@test.com")
                .activo(false)
                .build();

        LoginRequest req = LoginRequest.builder()
                .correo("juan@test.com")
                .password("Password123")
                .build();

        when(usuarioRepository.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () ->
                authService.login(req));
    }

    @Test
    @DisplayName("Login con usuario inexistente lanza CredencialesInvalidasException")
    void testLogin_UsuarioInexistente_LanzaExcepcion() {
        LoginRequest req = LoginRequest.builder()
                .correo("desconocido@test.com")
                .password("Password123")
                .build();

        when(usuarioRepository.findByCorreo("desconocido@test.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () ->
                authService.login(req));
    }
}
