package com.entelgy.bank.exception;

public class InvalidTokenException extends TokenValidationException  {

    public InvalidTokenException(String message) {
        super(message);
    }

}
