package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ActualizaCuentaRequest;
import com.proyecto.servicios.model.cliente.CrearCuentaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    CuentaResponseDto crearCuenta(CrearCuentaRequest request);

    CuentaResponseDto obtenerPorNumeroCuenta(String numeroCuenta);

    List<CuentaResponseDto> obtenerPorClienteId(Long clienteId);

    Page<CuentaResponseDto> obtenerPorClienteIdPaginado(Long clienteId, Pageable pageable);

    Page<CuentaResponseDto> obtenerPorEstatus(String estatus, Pageable pageable);

    Page<CuentaResponseDto> obtenerActivas(Pageable pageable);

    BigDecimal consultarSaldo(String numeroCuenta);

    CuentaResponseDto actualizarEstatus(String numeroCuenta, ActualizaCuentaRequest request);
}
