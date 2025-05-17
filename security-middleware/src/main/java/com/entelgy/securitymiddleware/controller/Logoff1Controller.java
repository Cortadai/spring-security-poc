package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import com.entelgy.securitymiddleware.repository.TokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Logoff1Controller {

    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;

    @GetMapping("/logoff1")
    public void logoff1(HttpServletRequest request, HttpServletResponse response) {
        String certHeader = request.getHeader("X-Cert-Auth");
        String idsession = request.getHeader("X-Idsession");

        if (certHeader == null || certHeader.isBlank() || idsession == null || idsession.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

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
            // 1. Decodificar tokens desde Base64
            String decodedSession = new String(Base64.getDecoder().decode(sessionCookieValue), StandardCharsets.UTF_8);
            String[] sessionParts = decodedSession.split("::");
            String jwtSesion = sessionParts[0];
            String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;
            String jwtAccess = new String(Base64.getDecoder().decode(accessCookieValue), StandardCharsets.UTF_8);

            // 2. Validar token de sesión
            if (!tokenProvider.validateToken(jwtSesion)) {
                log.warn("Token de sesión caducado, pero procedemos con el logout");
            }

            // 3. Obtener jti de access y refresh
            String jtiAccess = tokenProvider.getJtiFromToken(jwtAccess);
            String jtiRefresh = jwtRefresh != null ? tokenProvider.getJtiFromToken(jwtRefresh) : null;

            // 4. Revocar tokens en Redis
            tokenRepository.removeTokens(idsession, jtiAccess, jtiRefresh);

        } catch (Exception e) {
            log.warn("Error durante logoff1: {}", e.getMessage());
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
