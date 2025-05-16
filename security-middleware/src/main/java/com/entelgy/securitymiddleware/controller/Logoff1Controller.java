package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Logoff1Controller {

    private final TokenProvider tokenProvider;

    @GetMapping("/logoff1")
    public void logoff1(HttpServletRequest request, HttpServletResponse response) {

        // 1. Validar cabecera X-Cert-Auth
        String certHeader = request.getHeader("X-Cert-Auth");
        String idsession = request.getHeader("X-Idsession");

        if (certHeader == null || certHeader.isBlank() || idsession == null || idsession.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 2. Buscar cookies
        String sessionCookieName = "Session-" + idsession;
        String accessCookieName = "Acceso-" + idsession;

        String sessionCookieValue = null;
        String accessCookieValue = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionCookieValue = cookie.getValue();
                }
                if (accessCookieName.equals(cookie.getName())) {
                    accessCookieValue = cookie.getValue();
                }
            }
        }

        if (sessionCookieValue == null || accessCookieValue == null) {
            log.warn("Cookies no encontradas para idsession: {}", idsession);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // 3. Desencriptar y validar token de sesión
            String decodedSession = new String(Base64.getDecoder().decode(sessionCookieValue));
            String[] sessionParts = decodedSession.split("::");
            String jwtSesion = sessionParts[0];
            if (!tokenProvider.validateToken(jwtSesion)) {
                throw new RuntimeException("Token de sesión no válido");
            }

        } catch (Exception e) {
            log.warn("Error durante validación de tokens en logoff1: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 5. Crear cookies de borrado
        ResponseCookie deleteSession = ResponseCookie.from(sessionCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteAccess = ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        // 6. Devolver respuesta
        response.addHeader("Set-Cookie", deleteSession.toString());
        response.addHeader("Set-Cookie", deleteAccess.toString());
        response.setHeader("X-Idsession", "");
        response.setStatus(HttpServletResponse.SC_CREATED);
    }
}
