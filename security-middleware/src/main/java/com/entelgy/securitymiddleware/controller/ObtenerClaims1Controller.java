package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ObtenerClaims1Controller {

    private final TokenProvider tokenProvider;

    @GetMapping("/obtenerclaims1")
    public Map<String, Object> obtenerClaims(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");

        if (idsession == null || idsession.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Map.of("error", "Falta X-Idsession");
        }

        // Buscar cookies
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
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Faltan cookies de sesión o acceso");
        }

        try {
            // Desencriptar cookie de sesión: contiene session y refresh
            String decodedSession = new String(Base64.getDecoder().decode(sessionCookieValue));
            String[] sessionParts = decodedSession.split("::");
            String jwtSesion = sessionParts[0];
            String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;

            // Desencriptar cookie de acceso
            String jwtAccess = new String(Base64.getDecoder().decode(accessCookieValue));

            // Validar tokens
            if (!tokenProvider.validateToken(jwtSesion)) {
                throw new RuntimeException("Token de sesión inválido");
            }
            if (jwtRefresh != null && !tokenProvider.validateToken(jwtRefresh)) {
                throw new RuntimeException("Token de refresco inválido");
            }
            if (!tokenProvider.validateToken(jwtAccess)) {
                throw new RuntimeException("Token de acceso inválido");
            }

            // Extraer claims
            Claims sessionClaims = tokenProvider.parseToken(jwtSesion);
            Claims refreshClaims = jwtRefresh != null ? tokenProvider.parseToken(jwtRefresh) : null;
            Claims accessClaims = tokenProvider.parseToken(jwtAccess);

            Map<String, Object> result = new HashMap<>();
            result.put("session", sessionClaims);
            result.put("refresh", refreshClaims);
            result.put("access", accessClaims);

            response.setStatus(HttpServletResponse.SC_OK);
            return result;

        } catch (Exception e) {
            log.error("Error al obtener claims: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Tokens inválidos o corruptos");
        }
    }
}
