package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.ActualizaCuentaRequest;
import com.proyecto.servicios.model.cliente.CrearCuentaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.GeneradorCuentaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorCuentaUtil generadorCuentaUtil;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             ClienteRepository clienteRepository,
                             GeneradorCuentaUtil generadorCuentaUtil) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDto obtenerPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .map(CuentaResponseDto::fromEntity)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró ninguna cuenta con número: " + numeroCuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> obtenerPorClienteId(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNoEncontradoException("No se encontró ningún cliente con ID: " + clienteId);
        }
        return cuentaRepository.findByClienteId(clienteId)
                .stream()
                .map(CuentaResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CuentaResponseDto> obtenerPorClienteIdPaginado(Long clienteId, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNoEncontradoException("No se encontró ningún cliente con ID: " + clienteId);
        }
        return cuentaRepository.findByClienteId(clienteId, pageable)
                .map(CuentaResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CuentaResponseDto> obtenerPorEstatus(String estatus, Pageable pageable) {
        return cuentaRepository.findByEstatus(estatus.trim().toUpperCase(), pageable)
                .map(CuentaResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CuentaResponseDto> obtenerActivas(Pageable pageable) {
        return cuentaRepository.findByActivoTrue(pageable)
                .map(CuentaResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró ninguna cuenta con número: " + numeroCuenta));
        return cuenta.getSaldo();
    }

    @Override
    @Transactional
    public CuentaResponseDto actualizarEstatus(String numeroCuenta, ActualizaCuentaRequest request) {
        log.info("Actualizando estatus de cuenta: {} a {}", numeroCuenta, request.getEstatus());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
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
