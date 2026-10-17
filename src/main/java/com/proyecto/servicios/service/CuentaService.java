package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.ActualizaCuentaRequest;
import com.proyecto.servicios.model.cliente.CrearCuentaRequest;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.specification.CuentaSpecification;
import com.proyecto.servicios.util.GeneradorCuentaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorCuentaUtil generadorCuentaUtil;

    public CuentaService(CuentaRepository cuentaRepository,
                         ClienteRepository clienteRepository,
                         GeneradorCuentaUtil generadorCuentaUtil) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
    }

    @Transactional
    public CuentaResponseDto crearCuenta(CrearCuentaRequest request) {
        log.info("Creando cuenta adicional para cliente ID: {}", request.getClienteId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            log.warn("Rechazo de creación de cuenta: Cliente ID: {} se encuentra inactivo", cliente.getId());
            throw new ValidacionNegocioException("No se puede aperturar una cuenta para un cliente inactivo.");
        }

        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO;
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionNegocioException("El saldo inicial no puede ser negativo.");
        }

        String numeroCuenta = generadorCuentaUtil.generarNumeroCuentaUnico();

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .saldo(saldoInicial)
                .estatus("ACTIVA")
                .activo(true)
                .build();

        cuenta = cuentaRepository.save(cuenta);
        cliente.getCuentas().add(cuenta);

        log.info("Cuenta creada exitosamente con número: {} para cliente ID: {}", numeroCuenta, cliente.getId());
        return CuentaResponseDto.fromEntity(cuenta);
    }

    @Transactional(readOnly = true)
    public Page<CuentaResponseDto> consultarCuentas(CuentaConsultaRequest request) {
        log.info("Consultando cuentas con criterios dinámicos");

        int pagina = (request != null && request.getPagina() != null) ? request.getPagina() : 0;
        int tamano = (request != null && request.getTamanoPagina() != null) ? request.getTamanoPagina() : 20;
        String ordenarPor = (request != null && request.getOrdenarPor() != null && !request.getOrdenarPor().isBlank()) ? request.getOrdenarPor() : "id";
        Sort.Direction direccion = (request != null && "DESC".equalsIgnoreCase(request.getDireccionOrden())) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(direccion, ordenarPor));
        Specification<Cuenta> spec = CuentaSpecification.conFiltros(request);

        return cuentaRepository.findAll(spec, pageable).map(CuentaResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public CuentaResponseDto obtenerCuentaPorNumero(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .map(CuentaResponseDto::fromEntity)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró ninguna cuenta con número: " + numeroCuenta));
    }

    @Transactional
    public CuentaResponseDto actualizarEstatus(ActualizaCuentaRequest request) {
        String numeroCuenta = request.getNumeroCuenta().trim();
        log.info("Actualizando estatus de cuenta: {} a {}", numeroCuenta, request.getEstatus());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró ninguna cuenta con número: " + numeroCuenta));

        String nuevoEstatus = request.getEstatus().trim().toUpperCase();

        if ("ACTIVA".equalsIgnoreCase(nuevoEstatus)) {
            if (cuenta.getCliente() != null && !Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
                throw new ValidacionNegocioException("No se puede reactivar una cuenta cuyo cliente se encuentra inactivo.");
            }
            cuenta.setActivo(true);
        } else {
            cuenta.setActivo(false);
        }

        cuenta.setEstatus(nuevoEstatus);
        cuenta = cuentaRepository.save(cuenta);

        log.info("Estatus de cuenta {} actualizado exitosamente a {}", numeroCuenta, nuevoEstatus);
        return CuentaResponseDto.fromEntity(cuenta);
    }
}
