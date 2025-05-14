package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Login1EndController {

    private final TokenProvider tokenProvider;

    @GetMapping("/login1End")
    public void login1End(HttpServletRequest request, HttpServletResponse response) {

        // Paso 1: Obtener Sessiontmp
        String sessiontmp = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Sessiontmp".equals(cookie.getName())) {
                    sessiontmp = cookie.getValue();
                    break;
                }
            }
        }

        // Paso 2: Validar cabecera X-Cert-Auth
        String certHeader = request.getHeader("X-Cert-Auth");
        if (sessiontmp == null || sessiontmp.isBlank() || certHeader == null || certHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Paso 3 y 4: Extraer claims del JWT antes de validar
        Claims claims;
        try {
            claims = tokenProvider.parseToken(sessiontmp);
        } catch (Exception e) {
            log.warn("Error al parsear Sessiontmp antes de validar: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String idusuario = claims.getSubject();
        String idaplicacion = claims.get("idaplicacion", String.class);
        String idsession = claims.get("idsession", String.class);

        // Paso 5: Validar token JWT temporal
        if (!tokenProvider.validateToken(sessiontmp)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Paso 6: Generar token de sesión
        Map<String, Object> sessionClaims = new HashMap<>();
        sessionClaims.put("idaplicacion", idaplicacion);
        sessionClaims.put("idsession", idsession);
        sessionClaims.put("scope", "session");
        sessionClaims.put("refreshToken", true);
        String jwtSesion = tokenProvider.generateTemporalToken(idusuario, 3600_000, sessionClaims);

        // Paso 7: Validar token de sesión
        if (!tokenProvider.validateToken(jwtSesion)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Paso 8: Generar token de refresco
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("idaplicacion", idaplicacion);
        refreshClaims.put("idsession", idsession);
        refreshClaims.put("scope", "refresh");
        refreshClaims.put("maxRefresh", 3);
        String jwtRefresh = tokenProvider.generateTemporalToken(idusuario, 86_400_000, refreshClaims);

        // Paso 9: Validar token de refresco
        if (!tokenProvider.validateToken(jwtRefresh)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Paso 10: Concatenar y "cifrar" los tokens (simulado con Base64)
        String sessionCookieValue = Base64.getEncoder().encodeToString((jwtSesion + "::" + jwtRefresh).getBytes());
        String accessCookieValue = Base64.getEncoder().encodeToString(jwtSesion.getBytes());

        // Paso 11 y 12: Crear cookies
        ResponseCookie sessionCookie = ResponseCookie.from("Session-" + idsession, sessionCookieValue)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("Acceso-" + idsession, accessCookieValue)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        // Paso 13: Borrar Sessiontmp
        ResponseCookie deleteTmp = ResponseCookie.from("Sessiontmp", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        // Paso 14: Devolver cookies + X-Idsession + 201
        response.addHeader("Set-Cookie", sessionCookie.toString());
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", deleteTmp.toString());
        response.setHeader("X-Idsession", idsession);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

}
