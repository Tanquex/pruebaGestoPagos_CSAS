package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleIntegrationException(GestoPagoIntegrationException ex) {
        log.error("Excepción en integración externa: {}", ex.getMessage());
        GenericResponse errorResponse = new GenericResponse();
        errorResponse.setCodigo(ex.getCodigoError() != null ? ex.getCodigoError() : 500);
        errorResponse.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Error no controlado en la aplicación: ", ex);
        GenericResponse errorResponse = new GenericResponse();
        errorResponse.setCodigo(500);
        errorResponse.setMensaje("Ocurrió un error interno en el servicio");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
