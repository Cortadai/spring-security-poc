package com.entelgy.bank.model;

public record LoginResponse(String status, String accessToken, String refreshToken) {
}
