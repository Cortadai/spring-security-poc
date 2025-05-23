package com.example.bank.exception;

public class TokenMismatchException extends TokenValidationException  {

    public TokenMismatchException(String message) {
        super(message);
    }
    
}
