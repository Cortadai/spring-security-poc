package com.example.bank.exception;

public class TokenBlacklistedException extends TokenValidationException  {

    public TokenBlacklistedException(String message) {
        super(message);
    }
    
}
