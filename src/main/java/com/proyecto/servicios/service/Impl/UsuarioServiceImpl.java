package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              ClienteRepository clienteRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioResponseDto crearUsuario(CrearUsuarioRequest request) {
        log.info("Creando usuario para cliente ID: {}", request.getClienteId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("No se puede crear un usuario para un cliente inactivo.");
        }

        if (usuarioRepository.existsByClienteId(request.getClienteId())) {
            throw new ValidacionNegocioException("El cliente ya cuenta con un usuario de acceso registrado.");
        }

        String correo = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ClienteYaExisteException("El correo ya está registrado para otro usuario: " + correo);
        }

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(correo)
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Usuario creado exitosamente con ID: {} para cliente ID: {}", usuario.getId(), cliente.getId());
        return UsuarioResponseDto.fromEntity(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDto::fromEntity)
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró ningún usuario con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorClienteId(Long clienteId) {
        return usuarioRepository.findByClienteId(clienteId)
                .map(UsuarioResponseDto::fromEntity)
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró ningún usuario asociado al cliente ID: " + clienteId));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .map(UsuarioResponseDto::fromEntity)
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró ningún usuario con correo: " + correo));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> obtenerTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> obtenerPorActivo(Boolean activo, Pageable pageable) {
        return usuarioRepository.findByActivo(activo, pageable).map(UsuarioResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> buscarPorCorreo(String correo, Pageable pageable) {
        return usuarioRepository.findByCorreoContainingIgnoreCase(correo.trim(), pageable).map(UsuarioResponseDto::fromEntity);
    }
}
