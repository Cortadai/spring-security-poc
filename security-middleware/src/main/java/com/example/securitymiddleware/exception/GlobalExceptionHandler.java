package com.example.securitymiddleware.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error de argumento: " + e.getMessage());
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<String> handleTokenValidationException(TokenValidationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error de validación de token: " + e.getMessage());
    }

    @ExceptionHandler(ParameterNotFoundException.class)
    public ResponseEntity<String> handleMissingParameterException(ParameterNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parámetro faltante: " + e.getMessage());
    }

    @ExceptionHandler(CookieNotFoundException.class)
    public ResponseEntity<String> handleCookieNotFoundException(CookieNotFoundException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error de cookie: " + e.getMessage());
    }

    @ExceptionHandler(TokenParseException.class)
    public ResponseEntity<String> handleTokenParseException(TokenParseException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al parsear el token: " + e.getMessage());
    }

}