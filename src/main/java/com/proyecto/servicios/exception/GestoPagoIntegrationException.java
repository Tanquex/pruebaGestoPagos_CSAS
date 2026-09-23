package com.proyecto.servicios.exception;

public class GestoPagoIntegrationException extends RuntimeException {

    private final Integer codigoError;

    public GestoPagoIntegrationException(String message) {
        super(message);
        this.codigoError = null;
    }

    public GestoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.codigoError = null;
    }

    public GestoPagoIntegrationException(String message, Integer codigoError) {
        super(message);
        this.codigoError = codigoError;
    }

    public Integer getCodigoError() {
        return codigoError;
    }
}
