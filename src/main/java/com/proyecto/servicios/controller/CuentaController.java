package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ActualizaCuentaRequest;
import com.proyecto.servicios.model.cliente.CrearCuentaRequest;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponseDto;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Endpoints para la gestión, consulta dinámica unificada y modificación de cuentas bancarias asociadas")
@Slf4j
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear cuenta bancaria adicional",
            description = "Apertura una nueva cuenta bancaria para un cliente existente y activo, asignando número de cuenta único de 16 dígitos y saldo inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "422", description = "Cliente inactivo o saldo inicial negativo")
    })
    public ResponseEntity<CuentaResponseDto> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        log.info("Recibida petición POST /cuentas para clienteId: {}", request.getClienteId());
        CuentaResponseDto response = cuentaService.crearCuenta(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta dinámica unificada de cuentas bancarias (Paginado)",
            description = "Único endpoint para consultar cuentas permitiendo combinar filtros (número de cuenta, clienteId, estatus, activo) y consultar saldo en el cuerpo JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta o paginación inválidos")
    })
    public ResponseEntity<Page<CuentaResponseDto>> consultarCuentas(@Valid @RequestBody CuentaConsultaRequest request) {
        log.info("Recibida petición POST /cuentas/consultar");
        Page<CuentaResponseDto> response = cuentaService.consultarCuentas(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/estatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar estatus de una cuenta",
            description = "Modifica el estatus operativo (ACTIVA, INACTIVA, BLOQUEADA, CANCELADA) recibiendo el número de cuenta y nuevo estatus en el cuerpo JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatus de cuenta actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "422", description = "No se puede reactivar una cuenta con cliente inactivo")
    })
    public ResponseEntity<CuentaResponseDto> actualizarEstatus(@Valid @RequestBody ActualizaCuentaRequest request) {
        log.info("Recibida petición PATCH /cuentas/estatus para cuenta {}", request.getNumeroCuenta());
        CuentaResponseDto response = cuentaService.actualizarEstatus(request);
        return ResponseEntity.ok(response);
    }
}
