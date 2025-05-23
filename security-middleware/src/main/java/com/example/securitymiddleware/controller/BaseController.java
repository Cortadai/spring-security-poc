package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseController {

    protected final TokenProvider tokenProvider;

    protected String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    protected Claims parseValidToken(String token, String errorMsg) {
        if (!tokenProvider.validateToken(token)) throw new TokenValidationException(errorMsg);
        return tokenProvider.parseToken(token);
    }

}