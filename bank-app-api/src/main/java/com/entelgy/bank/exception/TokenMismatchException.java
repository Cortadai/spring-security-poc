package com.entelgy.bank.exception;

public class TokenMismatchException extends TokenValidationException  {

    public TokenMismatchException(String message) {
        super(message);
    }
    
}
