package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioConsultaRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.specification.UsuarioSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> consultarUsuarios(UsuarioConsultaRequest request) {
        log.info("Consultando usuarios con criterios dinámicos");

        int pagina = (request != null && request.getPagina() != null) ? request.getPagina() : 0;
        int tamano = (request != null && request.getTamanoPagina() != null) ? request.getTamanoPagina() : 20;
        String ordenarPor = (request != null && request.getOrdenarPor() != null && !request.getOrdenarPor().isBlank()) ? request.getOrdenarPor() : "id";
        Sort.Direction direccion = (request != null && "DESC".equalsIgnoreCase(request.getDireccionOrden())) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(direccion, ordenarPor));
        Specification<Usuario> spec = UsuarioSpecification.conFiltros(request);

        return usuarioRepository.findAll(spec, pageable).map(UsuarioResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDto::fromEntity)
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró ningún usuario con ID: " + id));
    }
}
