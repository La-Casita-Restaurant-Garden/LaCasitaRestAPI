package com.service.lacasita.exception;

/**
 * Excepción personalizada para errores de lógica de negocio.
 * Permite definir un código de estado HTTP personalizado.
 */
public class BusinessException extends RuntimeException {

    private final int statusCode;

    public BusinessException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BusinessException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
