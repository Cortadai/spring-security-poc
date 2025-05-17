package com.entelgy.securitymiddleware.exception;

public class TokenParseException extends RuntimeException {
    public TokenParseException(String message) {
        super(message);
    }
}
