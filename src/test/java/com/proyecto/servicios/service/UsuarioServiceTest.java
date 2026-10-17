package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioConsultaRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Crear usuario para cliente existente exitoso")
    void testCrearUsuario_Exitoso() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .activo(true)
                .build();

        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .clienteId(1L)
                .correo("nuevo.usuario@test.com")
                .password("PasswordSeguro123")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(usuarioRepository.existsByCorreo("nuevo.usuario@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPasswordHash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioResponseDto dto = usuarioService.crearUsuario(request);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("nuevo.usuario@test.com", dto.getCorreo());
        assertTrue(dto.getActivo());
    }

    @Test
    @DisplayName("Crear usuario para cliente inactivo lanza ValidacionNegocioException")
    void testCrearUsuario_ClienteInactivo_LanzaExcepcion() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .activo(false)
                .build();

        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .clienteId(1L)
                .correo("nuevo@test.com")
                .password("Password123")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(ValidacionNegocioException.class, () ->
                usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("Crear usuario con correo ya registrado lanza ClienteYaExisteException")
    void testCrearUsuario_CorreoDuplicado_LanzaExcepcion() {
        Cliente cliente = Cliente.builder().id(1L).activo(true).build();
        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .clienteId(1L)
                .correo("ya.existe@test.com")
                .password("Password123")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(usuarioRepository.existsByCorreo("ya.existe@test.com")).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () ->
                usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("Consultar usuarios dinámicamente con filtros combinados")
    void testConsultarUsuarios_Dinamico() {
        Usuario u = Usuario.builder()
                .id(1L)
                .correo("usuario@test.com")
                .activo(true)
                .build();

        Page<Usuario> page = new PageImpl<>(List.of(u));
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        UsuarioConsultaRequest req = UsuarioConsultaRequest.builder()
                .correo("usuario@test.com")
                .activo(true)
                .pagina(0)
                .tamanoPagina(10)
                .build();

        Page<UsuarioResponseDto> result = usuarioService.consultarUsuarios(req);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("usuario@test.com", result.getContent().get(0).getCorreo());
    }
}
