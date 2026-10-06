package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para la consulta y administración de credenciales de usuario")
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear usuario para cliente existente",
            description = "Permite registrar credenciales de acceso para un cliente que aún no cuenta con usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso"),
            @ApiResponse(responseCode = "422", description = "El cliente ya cuenta con usuario o está inactivo")
    })
    public ResponseEntity<UsuarioResponseDto> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        log.info("Recibida petición POST /usuarios para clienteId: {}", request.getClienteId());
        UsuarioResponseDto response = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<UsuarioResponseDto> obtenerPorId(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @GetMapping(value = "/cliente/{clienteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener usuario asociado a un cliente")
    public ResponseEntity<UsuarioResponseDto> obtenerPorClienteId(@PathVariable(name = "clienteId") Long clienteId) {
        return ResponseEntity.ok(usuarioService.obtenerPorClienteId(clienteId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar todos los usuarios (Paginado)")
    public ResponseEntity<Page<UsuarioResponseDto>> obtenerTodos(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.obtenerTodos(pageable));
    }

    @GetMapping(value = "/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Filtrar usuarios por estado activo o correo (Paginado)")
    public ResponseEntity<Page<UsuarioResponseDto>> filtrar(
            @RequestParam(name = "activo", required = false) Boolean activo,
            @RequestParam(name = "correo", required = false) String correo,
            @PageableDefault(size = 20) Pageable pageable) {

        if (activo != null) {
            return ResponseEntity.ok(usuarioService.obtenerPorActivo(activo, pageable));
        }
        if (correo != null && !correo.isBlank()) {
            return ResponseEntity.ok(usuarioService.buscarPorCorreo(correo, pageable));
        }
        return ResponseEntity.ok(usuarioService.obtenerTodos(pageable));
    }
}
