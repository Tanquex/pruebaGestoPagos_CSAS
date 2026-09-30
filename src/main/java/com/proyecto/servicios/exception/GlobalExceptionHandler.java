package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Error de validación en parámetros de entrada: {} errores detectados", ex.getBindingResult().getErrorCount());
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", HttpStatus.BAD_REQUEST.value());
        response.put("mensaje", "Error de validación en los datos enviados");
        response.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ClienteYaExisteException.class)
    public ResponseEntity<GenericResponse> handleClienteYaExiste(ClienteYaExisteException ex) {
        log.warn("Conflicto de cliente existente: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.CONFLICT.value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos en BD (posible duplicado bajo concurrencia masiva): {}", ex.getMostSpecificCause().getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.CONFLICT.value());

        String causeMsg = ex.getMostSpecificCause().getMessage();
        if (causeMsg != null && causeMsg.contains("uq_clientes_curp")) {
            response.setMensaje("Ya existe un cliente registrado con la misma CURP.");
        } else if (causeMsg != null && causeMsg.contains("uq_clientes_rfc")) {
            response.setMensaje("Ya existe un cliente registrado con el mismo RFC.");
        } else if (causeMsg != null && causeMsg.contains("uq_clientes_correo")) {
            response.setMensaje("Ya existe un cliente registrado con el mismo correo electrónico.");
        } else if (causeMsg != null && causeMsg.contains("uq_cuentas_numero")) {
            response.setMensaje("El número de cuenta ya se encuentra registrado.");
        } else {
            response.setMensaje("Conflicto de integridad en base de datos. Verifique que no existan valores duplicados.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            CuentaNoEncontradaException.class,
            UsuarioNoEncontradoException.class
    })
    public ResponseEntity<GenericResponse> handleNoEncontrado(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.NOT_FOUND.value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.UNAUTHORIZED.value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<GenericResponse> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleIntegrationException(GestoPagoIntegrationException ex) {
        log.error("Excepción en integración externa GestoPago: {}", ex.getMessage());
        GenericResponse errorResponse = new GenericResponse();
        errorResponse.setCodigo(ex.getCodigoError() != null ? ex.getCodigoError() : 500);
        errorResponse.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Error no controlado en la aplicación: ", ex);
        GenericResponse errorResponse = new GenericResponse();
        errorResponse.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setMensaje("Ocurrió un error interno en el servicio");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
