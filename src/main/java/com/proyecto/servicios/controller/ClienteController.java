package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Endpoints para el Onboarding integral, consultas y ciclo de vida de clientes personas físicas")
@Slf4j
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Onboarding de cliente persona física",
            description = "Registra un cliente, valida identidad y edad (>=18), crea cuenta bancaria con saldo inicial y genera credenciales cifradas con BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado y cuenta aperturada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato incorrecto"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o Correo ya registrados"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio no cumplida (ej. cliente menor de edad)")
    })
    public ResponseEntity<ClienteResponseDto> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        log.info("Recibida petición POST /clientes para CURP: {}", request.getCurp());
        ClienteResponseDto response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar todos los clientes paginados", description = "Devuelve página de clientes registrados. Satisface pruebas masivas.")
    public ResponseEntity<Page<ClienteResponseDto>> obtenerTodos(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clienteService.obtenerTodos(pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener cliente por ID", description = "Devuelve los datos completos del cliente por su clave primaria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ClienteResponseDto> obtenerPorId(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por CURP", description = "Búsqueda indexada por los 18 caracteres de la CURP")
    public ResponseEntity<ClienteResponseDto> obtenerPorCurp(@PathVariable(name = "curp") String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }

    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por RFC", description = "Búsqueda indexada por los 13 caracteres del RFC con homoclave")
    public ResponseEntity<ClienteResponseDto> obtenerPorRfc(@PathVariable(name = "rfc") String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }

    @GetMapping(value = "/correo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por correo electrónico")
    public ResponseEntity<ClienteResponseDto> obtenerPorCorreo(@RequestParam(name = "correo") String correo) {
        return ResponseEntity.ok(clienteService.obtenerPorCorreo(correo));
    }

    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por número de cuenta bancaria", description = "Búsqueda por los 16 dígitos de la cuenta bancaria")
    public ResponseEntity<ClienteResponseDto> obtenerPorNumeroCuenta(@PathVariable(name = "numeroCuenta") String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar clientes por nombre o apellidos (Paginado)",
            description = "Búsqueda case-insensitive por coincidencia en nombre, apellido paterno o apellido materno.")
    public ResponseEntity<Page<ClienteResponseDto>> buscar(
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "apellidoPaterno", required = false) String apellidoPaterno,
            @RequestParam(name = "apellidoMaterno", required = false) String apellidoMaterno,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        if (nombre != null && !nombre.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarPorNombre(nombre, pageable));
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarPorApellidoPaterno(apellidoPaterno, pageable));
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarPorApellidoMaterno(apellidoMaterno, pageable));
        }
        return ResponseEntity.ok(clienteService.obtenerTodos(pageable));
    }

    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar clientes activos (Paginado)")
    public ResponseEntity<Page<ClienteResponseDto>> obtenerActivos(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clienteService.obtenerActivos(pageable));
    }

    @GetMapping(value = "/fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar clientes por rango de fechas de creación (Paginado)",
            description = "Filtro por fecha de creación (formato ISO: YYYY-MM-DDTHH:mm:ss)")
    public ResponseEntity<Page<ClienteResponseDto>> obtenerPorFechas(
            @RequestParam(name = "inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(name = "fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(inicio, fin, pageable));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualización parcial de cliente",
            description = "Permite modificar datos personales, de contacto, laborales y domicilio. Prohíbe estrictamente modificar CURP y RFC.")
    public ResponseEntity<ClienteResponseDto> actualizarParcial(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ActualizaClienteRequest request) {
        log.info("Recibida petición PATCH /clientes/{} para actualización parcial", id);
        return ResponseEntity.ok(clienteService.actualizarParcial(id, request));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Baja lógica de cliente en cascada",
            description = "Desactiva el cliente (activo = false) e inactiva automáticamente todas sus cuentas bancarias y su usuario asociado.")
    public ResponseEntity<Void> darDeBajaLogica(@PathVariable(name = "id") Long id) {
        log.info("Recibida petición DELETE /clientes/{} para baja lógica", id);
        clienteService.darDeBajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
