package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.PagoServicioRequest;
import com.proyecto.servicios.model.cliente.PagoServicioResponseDto;
import com.proyecto.servicios.model.cliente.TransaccionConsultaRequest;
import com.proyecto.servicios.service.PagoServicioService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos de Servicios y Tiempo Aire", description = "Endpoints para el procesamiento seguro de transacciones hacia GestoPago (sendTx.do)")
@Slf4j
public class PagoServicioController {

    private final PagoServicioService pagoServicioService;

    public PagoServicioController(PagoServicioService pagoServicioService) {
        this.pagoServicioService = pagoServicioService;
    }

    @PostMapping(value = "/transaccion", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Procesar pago de servicio o recarga de tiempo aire (GestoPago sendTx.do)",
            description = "Valida cuenta activa, saldo, PIN seguro con BCrypt, catálogo en MongoDB, firma payload con AES y descuenta saldo SOLO si GestoPago confirma con código 01.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o producto/monto no coincide con catálogo"),
            @ApiResponse(responseCode = "401", description = "PIN o credenciales de autorización incorrectas"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Folio o llave de idempotencia duplicada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente o cuenta/cliente inactivo"),
            @ApiResponse(responseCode = "504", description = "Tiempo de espera agotado con el proveedor externo")
    })
    public ResponseEntity<PagoServicioResponseDto> procesarPago(@Valid @RequestBody PagoServicioRequest request) {
        log.info("Recibida petición POST /pagos/transaccion");
        PagoServicioResponseDto response = pagoServicioService.procesarPagoServicio(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta dinámica unificada de transacciones (Paginado)",
            description = "Único endpoint para consultar historial de pagos y conciliación permitiendo combinar filtros (ID, cuenta, randomString, servicio, producto, estatus, fechas) en el cuerpo JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda de transacciones procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos")
    })
    public ResponseEntity<Page<PagoServicioResponseDto>> consultarTransacciones(@Valid @RequestBody TransaccionConsultaRequest request) {
        log.info("Recibida petición POST /pagos/consultar");
        Page<PagoServicioResponseDto> response = pagoServicioService.consultarTransacciones(request);
        return ResponseEntity.ok(response);
    }
}
