package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioConsultaRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioService;
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
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para la creación y consulta dinámica unificada de credenciales de usuario")
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear usuario para cliente existente",
            description = "Registra credenciales de acceso para un cliente que aún no cuenta con usuario.")
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

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta dinámica unificada de usuarios (Paginado)",
            description = "Único endpoint para consultar usuarios permitiendo combinar filtros (ID, clienteId, correo, activo) en el cuerpo JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios consultados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos")
    })
    public ResponseEntity<Page<UsuarioResponseDto>> consultarUsuarios(@Valid @RequestBody UsuarioConsultaRequest request) {
        log.info("Recibida petición POST /usuarios/consultar");
        Page<UsuarioResponseDto> response = usuarioService.consultarUsuarios(request);
        return ResponseEntity.ok(response);
    }
}
