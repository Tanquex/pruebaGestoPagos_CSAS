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
import com.proyecto.servicios.util.GeneradorCuentaUtil;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private GeneradorCuentaUtil generadorCuentaUtil;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    @DisplayName("Crear cuenta adicional para cliente activo exitoso")
    void testCrearCuenta_Exitoso() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .activo(true)
                .cuentas(new ArrayList<>())
                .build();

        CrearCuentaRequest request = CrearCuentaRequest.builder()
                .clienteId(1L)
                .saldoInicial(new BigDecimal("500.00"))
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(generadorCuentaUtil.generarNumeroCuentaUnico()).thenReturn("4152998877665544");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> {
            Cuenta c = i.getArgument(0);
            c.setId(10L);
            return c;
        });

        CuentaResponseDto dto = cuentaService.crearCuenta(request);

        assertNotNull(dto);
        assertEquals("4152998877665544", dto.getNumeroCuenta());
        assertEquals(new BigDecimal("500.00"), dto.getSaldo());
        assertEquals("ACTIVA", dto.getEstatus());
        assertTrue(dto.getActivo());
    }

    @Test
    @DisplayName("Crear cuenta rechazada si cliente está inactivo")
    void testCrearCuenta_ClienteInactivo_LanzaExcepcion() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .activo(false)
                .build();

        CrearCuentaRequest request = CrearCuentaRequest.builder()
                .clienteId(1L)
                .saldoInicial(BigDecimal.ZERO)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(ValidacionNegocioException.class, () ->
                cuentaService.crearCuenta(request));
    }

    @Test
    @DisplayName("Consultar cuentas dinámicamente con filtros combinados")
    void testConsultarCuentas_Dinamico() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("4152000011112222")
                .saldo(new BigDecimal("2500.50"))
                .estatus("ACTIVA")
                .activo(true)
                .build();

        Page<Cuenta> page = new PageImpl<>(List.of(cuenta));
        when(cuentaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CuentaConsultaRequest queryReq = CuentaConsultaRequest.builder()
                .numeroCuenta("4152000011112222")
                .estatus("ACTIVA")
                .pagina(0)
                .tamanoPagina(10)
                .build();

        Page<CuentaResponseDto> result = cuentaService.consultarCuentas(queryReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("4152000011112222", result.getContent().get(0).getNumeroCuenta());
        assertEquals(new BigDecimal("2500.50"), result.getContent().get(0).getSaldo());
    }

    @Test
    @DisplayName("Consultar cuenta por número de cuenta existente exitoso")
    void testObtenerCuentaPorNumero_Exitoso() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("4152000011112222")
                .saldo(new BigDecimal("2500.50"))
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152000011112222")).thenReturn(Optional.of(cuenta));

        CuentaResponseDto dto = cuentaService.obtenerCuentaPorNumero("4152000011112222");
        assertEquals(new BigDecimal("2500.50"), dto.getSaldo());
    }

    @Test
    @DisplayName("Actualizar estatus a BLOQUEADA exitoso con DTO")
    void testActualizarEstatus_Bloqueada() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("4152000011112222")
                .estatus("ACTIVA")
                .activo(true)
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152000011112222")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        ActualizaCuentaRequest req = ActualizaCuentaRequest.builder()
                .numeroCuenta("4152000011112222")
                .estatus("BLOQUEADA")
                .build();
        CuentaResponseDto dto = cuentaService.actualizarEstatus(req);

        assertEquals("BLOQUEADA", dto.getEstatus());
        assertFalse(dto.getActivo());
    }
}
