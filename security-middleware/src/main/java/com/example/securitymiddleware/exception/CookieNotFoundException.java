package com.example.securitymiddleware.exception;

public class CookieNotFoundException extends RuntimeException {

    public CookieNotFoundException(String message) {
        super(message);
    }

}
