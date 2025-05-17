package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final TokenProvider tokenProvider;

    @GetMapping("/estadosession")
    public void estadoSesion(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");
        String certHeader = request.getHeader("X-Cert-Auth");

        if (idsession == null || idsession.isBlank() || certHeader == null || certHeader.isBlank()) {
            log.warn("Falta X-Idsession o X-Cert-Auth");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Buscar cookie "Session-{idsession}"
        String sessionCookieName = "Session-" + idsession;
        String sessionCookieValue = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionCookieValue = cookie.getValue();
                    break;
                }
            }
        }

        if (sessionCookieValue == null) {
            log.warn("Cookie de sesión no encontrada");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // Desencriptar y separar tokens
            String decoded = new String(Base64.getDecoder().decode(sessionCookieValue), StandardCharsets.UTF_8);
            String[] parts = decoded.split("::");
            if (parts.length < 2) {
                log.warn("Cookie de sesión mal formada (faltan tokens)");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String jwtSession = parts[0];
            String jwtRefresh = parts[1];

            // Validar tokens
            if (!tokenProvider.validateToken(jwtSession)) {
                log.warn("Token de sesión inválido");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!tokenProvider.validateToken(jwtRefresh)) {
                log.warn("Token de refresco inválido o revocado");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Sesión activa
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            log.error("Error al validar sesión: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
