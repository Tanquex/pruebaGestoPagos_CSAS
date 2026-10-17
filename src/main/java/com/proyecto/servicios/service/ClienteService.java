package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.BajaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.specification.ClienteSpecification;
import com.proyecto.servicios.util.GeneradorCuentaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DomicilioRepository domicilioRepository;
    private final GeneradorCuentaUtil generadorCuentaUtil;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository,
                          CuentaRepository cuentaRepository,
                          UsuarioRepository usuarioRepository,
                          DomicilioRepository domicilioRepository,
                          GeneradorCuentaUtil generadorCuentaUtil,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.domicilioRepository = domicilioRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ClienteResponseDto registrarCliente(RegistroClienteRequest request) {
        log.info("Iniciando proceso de Onboarding para cliente con CURP: {}", request.getCurp());

        // 1. Validar mayoría de edad (18 años o más)
        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Rechazo de registro: Cliente menor de edad ({} años). CURP={}", edad, request.getCurp());
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 años o más). Edad calculada: " + edad + " años.");
        }

        // 2. Validar reglas de unicidad
        if (clienteRepository.existsByCurp(request.getCurp())) {
            log.warn("Rechazo de registro: CURP ya registrada: {}", request.getCurp());
            throw new ClienteYaExisteException("Ya existe un cliente registrado con la CURP: " + request.getCurp());
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            log.warn("Rechazo de registro: RFC ya registrado: {}", request.getRfc());
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el RFC: " + request.getRfc());
        }
        if (clienteRepository.existsByCorreo(request.getCorreo())) {
            log.warn("Rechazo de registro: Correo ya registrado: {}", request.getCorreo());
            throw new ClienteYaExisteException("Ya existe un cliente registrado con el correo: " + request.getCorreo());
        }

        // 3. Crear Domicilio y Cliente
        Domicilio domicilio = request.getDomicilio().toEntity();
        domicilio = domicilioRepository.save(domicilio);

        Cliente cliente = Cliente.builder()
                .domicilio(domicilio)
                .nombre(request.getNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .curp(request.getCurp().trim().toUpperCase())
                .rfc(request.getRfc().trim().toUpperCase())
                .sexo(request.getSexo().trim().toUpperCase())
                .nacionalidad(request.getNacionalidad() != null ? request.getNacionalidad().trim() : "Mexicana")
                .estadoCivil(request.getEstadoCivil().trim().toUpperCase())
                .correo(request.getCorreo().trim().toLowerCase())
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null)
                .ocupacion(request.getOcupacion().trim())
                .empresa(request.getEmpresa().trim())
                .ingresoMensual(request.getIngresoMensual())
                .activo(true)
                .build();

        cliente = clienteRepository.save(cliente);

        // 4. Crear Cuenta bancaria automática asociada
        String numeroCuenta = generadorCuentaUtil.generarNumeroCuentaUnico();
        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO;

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .saldo(saldoInicial)
                .estatus("ACTIVA")
                .activo(true)
                .build();

        cuenta = cuentaRepository.save(cuenta);
        cliente.getCuentas().add(cuenta);

        // 5. Crear Usuario de acceso asociado con contraseña BCrypt
        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(request.getCorreo().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Onboarding completado exitosamente para clienteId={}, cuenta={}, usuario={}",
                cliente.getId(), numeroCuenta, usuario.getCorreo());

        return ClienteResponseDto.fromEntity(cliente);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponseDto> consultarClientes(ClienteConsultaRequest request) {
        log.info("Consultando clientes con criterios dinámicos");

        int pagina = (request != null && request.getPagina() != null) ? request.getPagina() : 0;
        int tamano = (request != null && request.getTamanoPagina() != null) ? request.getTamanoPagina() : 20;
        String ordenarPor = (request != null && request.getOrdenarPor() != null && !request.getOrdenarPor().isBlank()) ? request.getOrdenarPor() : "id";
        Sort.Direction direccion = (request != null && "DESC".equalsIgnoreCase(request.getDireccionOrden())) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(direccion, ordenarPor));
        Specification<Cliente> spec = ClienteSpecification.conFiltros(request);

        return clienteRepository.findAll(spec, pageable).map(ClienteResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteResponseDto::fromEntity)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con ID: " + id));
    }

    @Transactional
    public ClienteResponseDto actualizarParcial(ActualizaClienteRequest request) {
        Long id = request.getId();
        log.info("Actualizando información parcial para cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con ID: " + id));

        // Regla: No se permite modificar CURP ni RFC ni número de cuenta (campos controlados por el servidor)
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            cliente.setNombre(request.getNombre().trim());
        }
        if (request.getSegundoNombre() != null) {
            cliente.setSegundoNombre(request.getSegundoNombre().trim());
        }
        if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().isBlank()) {
            cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        }
        if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().isBlank()) {
            cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        }
        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            String nuevoCorreo = request.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreo()) && clienteRepository.existsByCorreo(nuevoCorreo)) {
                throw new ClienteYaExisteException("El correo ya está en uso por otro cliente: " + nuevoCorreo);
            }
            cliente.setCorreo(nuevoCorreo);
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setCorreo(nuevoCorreo);
            }
        }
        if (request.getTelefonoMovil() != null && !request.getTelefonoMovil().isBlank()) {
            cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        }
        if (request.getTelefonoAlternativo() != null) {
            cliente.setTelefonoAlternativo(request.getTelefonoAlternativo().trim());
        }
        if (request.getEstadoCivil() != null && !request.getEstadoCivil().isBlank()) {
            cliente.setEstadoCivil(request.getEstadoCivil().trim().toUpperCase());
        }
        if (request.getOcupacion() != null && !request.getOcupacion().isBlank()) {
            cliente.setOcupacion(request.getOcupacion().trim());
        }
        if (request.getEmpresa() != null && !request.getEmpresa().isBlank()) {
            cliente.setEmpresa(request.getEmpresa().trim());
        }
        if (request.getIngresoMensual() != null) {
            cliente.setIngresoMensual(request.getIngresoMensual());
        }
        if (request.getDomicilio() != null && cliente.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (request.getDomicilio().getCalle() != null) dom.setCalle(request.getDomicilio().getCalle().trim());
            if (request.getDomicilio().getNumeroExterior() != null) dom.setNumeroExterior(request.getDomicilio().getNumeroExterior().trim());
            if (request.getDomicilio().getNumeroInterior() != null) dom.setNumeroInterior(request.getDomicilio().getNumeroInterior().trim());
            if (request.getDomicilio().getColonia() != null) dom.setColonia(request.getDomicilio().getColonia().trim());
            if (request.getDomicilio().getMunicipio() != null) dom.setMunicipio(request.getDomicilio().getMunicipio().trim());
            if (request.getDomicilio().getEstado() != null) dom.setEstado(request.getDomicilio().getEstado().trim());
            if (request.getDomicilio().getCodigoPostal() != null) dom.setCodigoPostal(request.getDomicilio().getCodigoPostal().trim());
            if (request.getDomicilio().getPais() != null) dom.setPais(request.getDomicilio().getPais().trim());
        }

        cliente = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado exitosamente", id);
        return ClienteResponseDto.fromEntity(cliente);
    }

    @Transactional
    public void darDeBajaLogica(BajaClienteRequest request) {
        Long id = request.getId();
        log.info("Ejecutando baja lógica para cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente con ID: " + id));

        // Regla: Desactivar cliente
        cliente.setActivo(false);

        // Regla: Desactivar cuentas en cascada
        if (cliente.getCuentas() != null) {
            for (Cuenta cuenta : cliente.getCuentas()) {
                cuenta.setActivo(false);
                cuenta.setEstatus("INACTIVA");
            }
        }

        // Regla: Desactivar usuario en cascada
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica completada exitosamente para cliente ID: {}", id);
    }
}
