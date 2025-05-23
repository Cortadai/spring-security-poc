package com.example.bank.exception;

public class TokenAuthenticationException extends TokenValidationException  {

    public TokenAuthenticationException(String message) {
        super(message);
    }

}
