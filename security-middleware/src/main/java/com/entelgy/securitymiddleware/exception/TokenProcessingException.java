package com.entelgy.securitymiddleware.exception;

public class TokenProcessingException extends RuntimeException {
    public TokenProcessingException(String message) {
        super(message);
    }
}
