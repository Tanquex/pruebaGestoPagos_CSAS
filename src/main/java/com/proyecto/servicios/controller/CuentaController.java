package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ActualizaCuentaRequest;
import com.proyecto.servicios.model.cliente.CrearCuentaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponseDto;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Endpoints para la gestión, consultas y modificación de cuentas bancarias asociadas")
@Slf4j
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear cuenta bancaria adicional",
            description = "Apertura una nueva cuenta bancaria para un cliente existente y activo, asignando número de cuenta único y saldo inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "422", description = "Cliente inactivo o saldo inicial inválido")
    })
    public ResponseEntity<CuentaResponseDto> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        log.info("Recibida petición POST /cuentas para clienteId: {}", request.getClienteId());
        CuentaResponseDto response = cuentaService.crearCuenta(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener cuenta por número de cuenta", description = "Consulta los datos de la cuenta por sus 16 dígitos")
    public ResponseEntity<CuentaResponseDto> obtenerPorNumeroCuenta(@PathVariable(name = "numeroCuenta") String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping(value = "/cliente/{clienteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar cuentas por cliente (Paginado)")
    public ResponseEntity<Page<CuentaResponseDto>> obtenerPorClienteId(
            @PathVariable(name = "clienteId") Long clienteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cuentaService.obtenerPorClienteIdPaginado(clienteId, pageable));
    }

    @GetMapping(value = "/estatus/{estatus}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar cuentas por estatus operativo (Paginado)", description = "Estatus disponibles: ACTIVA, INACTIVA, BLOQUEADA, CANCELADA")
    public ResponseEntity<Page<CuentaResponseDto>> obtenerPorEstatus(
            @PathVariable(name = "estatus") String estatus,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cuentaService.obtenerPorEstatus(estatus, pageable));
    }

    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar todas las cuentas activas (Paginado)")
    public ResponseEntity<Page<CuentaResponseDto>> obtenerActivas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cuentaService.obtenerActivas(pageable));
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar saldo disponible de una cuenta")
    public ResponseEntity<Map<String, Object>> consultarSaldo(@PathVariable(name = "numeroCuenta") String numeroCuenta) {
        BigDecimal saldo = cuentaService.consultarSaldo(numeroCuenta);
        return ResponseEntity.ok(Map.of(
                "numeroCuenta", numeroCuenta,
                "saldo", saldo
        ));
    }

    @PatchMapping(value = "/{numeroCuenta}/estatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar estatus de una cuenta",
            description = "Modifica el estatus operativo (ACTIVA, INACTIVA, BLOQUEADA, CANCELADA). Si se activa, valida que el cliente esté activo.")
    public ResponseEntity<CuentaResponseDto> actualizarEstatus(
            @PathVariable(name = "numeroCuenta") String numeroCuenta,
            @Valid @RequestBody ActualizaCuentaRequest request) {
        log.info("Recibida petición PATCH /cuentas/{}/estatus con estatus {}", numeroCuenta, request.getEstatus());
        return ResponseEntity.ok(cuentaService.actualizarEstatus(numeroCuenta, request));
    }
}
