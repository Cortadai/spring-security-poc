package com.example.bank.exception;

import org.springframework.security.core.AuthenticationException;

public class MiddlewareException extends AuthenticationException {

    public MiddlewareException(String message) {
        super(message);
    }

}
