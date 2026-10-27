package com.proyecto.servicios.exception;

public class ValidacionNegocioException extends RuntimeException {
    public ValidacionNegocioException(String message) {
        super(message);
    }

    public ValidacionNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
