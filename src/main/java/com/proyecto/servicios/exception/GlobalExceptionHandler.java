package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Error de validación en parámetros de entrada: {} errores detectados", ex.getBindingResult().getErrorCount());
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Error de validación en los datos de entrada")
                .errores(errores)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de solicitud JSON malformado o ilegible: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("El formato del cuerpo JSON de la solicitud es inválido")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({ClienteYaExisteException.class, TransaccionDuplicadaException.class})
    public ResponseEntity<ErrorResponseDto> handleClienteYaExiste(RuntimeException ex) {
        log.warn("Conflicto de cliente existente: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.CONFLICT.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos en BD: {}", ex.getMostSpecificCause().getMessage());
        String mensaje = "Conflicto de integridad en base de datos. Verifique que no existan valores duplicados.";
        String causeMsg = ex.getMostSpecificCause().getMessage();
        if (causeMsg != null && causeMsg.contains("uq_clientes_curp")) {
            mensaje = "Ya existe un cliente registrado con la misma CURP.";
        } else if (causeMsg != null && causeMsg.contains("uq_clientes_rfc")) {
            mensaje = "Ya existe un cliente registrado con el mismo RFC.";
        } else if (causeMsg != null && causeMsg.contains("uq_clientes_correo")) {
            mensaje = "Ya existe un cliente registrado con el mismo correo electrónico.";
        } else if (causeMsg != null && causeMsg.contains("uq_cuentas_numero")) {
            mensaje = "El número de cuenta ya se encuentra registrado.";
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.CONFLICT.value())
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            CuentaNoEncontradaException.class,
            UsuarioNoEncontradoException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNoEncontrado(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.NOT_FOUND.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDto> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.UNAUTHORIZED.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponseDto> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(GestoPagoResultadoInciertoException.class)
    public ResponseEntity<ErrorResponseDto> handleResultadoIncierto(GestoPagoResultadoInciertoException ex) {
        log.error("Resultado incierto en integración con GestoPago: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.GATEWAY_TIMEOUT.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<ErrorResponseDto> handleIntegrationException(GestoPagoIntegrationException ex) {
        log.error("Excepción en integración externa GestoPago: {}", ex.getMessage());
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.BAD_GATEWAY.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.NOT_FOUND.value())
                .mensaje("Recurso o ruta no encontrada: " + ex.getResourcePath())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        log.error("Error no controlado en la aplicación: ", ex);
        ErrorResponseDto response = ErrorResponseDto.builder()
                .codigo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .mensaje("Ocurrió un error interno al procesar la solicitud.")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
