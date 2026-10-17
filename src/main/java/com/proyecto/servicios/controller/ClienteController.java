package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.BajaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import com.proyecto.servicios.service.ClienteService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Endpoints para el Onboarding, consulta dinámica unificada y administración de clientes")
@Slf4j
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Onboarding integral de cliente persona física",
            description = "Registra un nuevo cliente validando mayoría de edad, unicidad de CURP/RFC/Correo, y genera automáticamente su cuenta bancaria de 16 dígitos y usuario de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado y aperturado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o Correo ya registrados"),
            @ApiResponse(responseCode = "422", description = "Cliente menor de edad")
    })
    public ResponseEntity<ClienteResponseDto> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        log.info("Recibida petición POST /clientes para CURP: {}", request.getCurp());
        ClienteResponseDto response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta dinámica unificada de clientes (Paginado)",
            description = "Único endpoint para consultar y buscar clientes permitiendo combinar cualquier filtro (ID, CURP, RFC, Correo, Número de Cuenta, Nombre, Apellidos, Activo, Rango de Fechas) recibido en el cuerpo JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta o paginación inválidos")
    })
    public ResponseEntity<Page<ClienteResponseDto>> consultarClientes(@Valid @RequestBody ClienteConsultaRequest request) {
        log.info("Recibida petición POST /clientes/consultar");
        Page<ClienteResponseDto> response = clienteService.consultarClientes(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualización parcial de cliente",
            description = "Modifica datos personales, laborales, de contacto y domicilio recibiendo el ID en el cuerpo JSON. Prohíbe modificar campos controlados por el servidor como CURP y RFC.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o ID ausente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "409", description = "El nuevo correo ya está en uso")
    })
    public ResponseEntity<ClienteResponseDto> actualizarParcial(@Valid @RequestBody ActualizaClienteRequest request) {
        log.info("Recibida petición PATCH /clientes para ID: {}", request.getId());
        ClienteResponseDto response = clienteService.actualizarParcial(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Baja lógica de cliente en cascada",
            description = "Desactiva el cliente (activo = false) e inactiva automáticamente todas sus cuentas bancarias asociadas y su usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente y productos asociados dados de baja exitosamente"),
            @ApiResponse(responseCode = "400", description = "ID de cliente inválido o ausente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<Void> darDeBajaLogica(@Valid @RequestBody BajaClienteRequest request) {
        log.info("Recibida petición DELETE /clientes para ID: {}", request.getId());
        clienteService.darDeBajaLogica(request);
        return ResponseEntity.noContent().build();
    }
}
