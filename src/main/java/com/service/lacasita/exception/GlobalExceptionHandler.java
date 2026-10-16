package com.service.lacasita.exception;

import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/**
 * Manejador global de excepciones para la API REST.
 * Centraliza el manejo de errores y devuelve respuestas consistentes.
 *
 * @author Jason D. Nuñez
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Response<String>> handleEntityNotFound(EntityNotFoundException ex) {
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(404, ex.getMessage())),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<String>> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(400, ex.getMessage())),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation error");

        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(400, message)),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Rol insuficiente. Cubre también {@code AuthorizationDeniedException}, que es
     * la que lanza la seguridad por método en Spring Security 6 y hereda de esta.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(403,
                        "No tiene permisos para realizar esta operación")),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<String>> handleNoResource(NoResourceFoundException ex) {
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(404, "Recurso no encontrado")),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<String>> handleBusinessException(BusinessException ex) {
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(ex.getStatusCode(), ex.getMessage())),
                HttpStatus.valueOf(ex.getStatusCode()));
    }

    /**
     * Conflicto de escritura concurrente: dos usuarios modificaron la misma fila a
     * la vez y @Version detectó el choque. 409 le dice al cliente que recargue y
     * reintente, en vez de dejarlo creer que su cambio se guardó.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Response<String>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(409,
                        "El registro fue modificado por otro usuario. Recargue y vuelva a intentar.")),
                HttpStatus.CONFLICT);
    }

    /**
     * Violación de constraint (unique, FK, check). El mensaje de Postgres trae
     * nombres de constraint y fragmentos de SQL, así que se registra en el log pero
     * no se devuelve al cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response<String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(409,
                        "La operación viola una restricción de datos. Verifique la información enviada.")),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<String>> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Error no controlado [{}] ({}): {}",
                errorId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(500,
                        "Error interno del servidor (ref: " + errorId + ")")),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
