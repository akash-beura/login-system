package com.akash.loginsystem.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorBody> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorBody> handleInvalidCredentials(InvalidCredentialsException ex) {
        // Log failure but don't leak detail — see conventions.md
        log.warn("Authentication failure: {}", ex.getMessage());
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorBody> handlePasswordMismatch(PasswordMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PasswordNotSetException.class)
    public ResponseEntity<ErrorBody> handlePasswordNotSet(PasswordNotSetException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(PasswordAlreadySetException.class)
    public ResponseEntity<ErrorBody> handlePasswordAlreadySet(PasswordAlreadySetException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OAuthCodeExpiredException.class)
    public ResponseEntity<ErrorBody> handleOAuthCodeExpired(OAuthCodeExpiredException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Bean-validation failures — returns field-level error map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        ErrorBody body = new ErrorBody(HttpStatus.BAD_REQUEST.value(), "Validation failed", Instant.now(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorBody> body(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorBody(status.value(), message, Instant.now(), null));
    }

    public record ErrorBody(int status, String message, Instant timestamp, Map<String, String> errors) {}
}
