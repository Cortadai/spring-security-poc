package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import com.entelgy.securitymiddleware.repository.TokenRepository;
import com.entelgy.securitymiddleware.service.SecurityInfoService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Login1EndController {

    @Value("${jwt.maxRefresh}")
    private int maxRefresh;

    private final TokenProvider tokenProvider;
    private final SecurityInfoService securityInfoService;
    private final TokenRepository tokenRepository;

    @GetMapping("/login1End")
    public void login1End(HttpServletRequest request, HttpServletResponse response) {
        String sessiontmp = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Sessiontmp".equals(cookie.getName())) {
                    sessiontmp = cookie.getValue();
                    break;
                }
            }
        }

        String certHeader = request.getHeader("X-Cert-Auth");
        if (sessiontmp == null || sessiontmp.isBlank() || certHeader == null || certHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Claims claims;
        try {
            claims = tokenProvider.parseToken(sessiontmp);
        } catch (Exception e) {
            log.warn("Error al parsear Sessiontmp: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String idusuario = claims.getSubject();
        String idaplicacion = claims.get("idaplicacion", String.class);
        String idsession = claims.get("idsession", String.class);

        if (!tokenProvider.validateToken(sessiontmp)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Generar tokens
        String jwtSesion = tokenProvider.generateSessionToken(idusuario, idaplicacion, idsession);
        String jwtRefresh = tokenProvider.generateRefreshToken(idusuario, idaplicacion, idsession, maxRefresh);
        List<String> roles = securityInfoService.getRolesForUser(idusuario, idaplicacion);
        String jwtAcceso = tokenProvider.generateAccessToken(idusuario, idaplicacion, idsession, roles, 0);

        // Guardar en Redis los tokens access y refresh
        tokenRepository.storeAccessToken(idsession, jwtAcceso);
        tokenRepository.storeRefreshToken(idsession, jwtRefresh);

        // Codificar para las cookies
        String sessionCookieValue = Base64.getEncoder().encodeToString((jwtSesion + "::" + jwtRefresh).getBytes(StandardCharsets.UTF_8));
        String accessCookieValue = Base64.getEncoder().encodeToString(jwtAcceso.getBytes(StandardCharsets.UTF_8));

        // Crear cookies
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

        ResponseCookie deleteTmp = ResponseCookie.from("Sessiontmp", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        // Devolver respuesta
        response.addHeader("Set-Cookie", sessionCookie.toString());
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", deleteTmp.toString());
        response.setHeader("X-Idsession", idsession);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }
}
