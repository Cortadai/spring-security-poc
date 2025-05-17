package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExpiraTokenController {

    private final TokenProvider tokenProvider;

    @GetMapping("/expira1")
    public ResponseEntity<Boolean> tokenPorExpirar(HttpServletRequest request) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            log.warn("Falta X-Idsession en la cabecera");
            return ResponseEntity.badRequest().body(false);
        }

        String accesoCookieName = "Acceso-" + idsession;
        String jwtAccess = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (accesoCookieName.equals(cookie.getName())) {
                    try {
                        jwtAccess = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.error("Error al decodificar Base64 de la cookie de acceso: {}", e.getMessage());
                        return ResponseEntity.status(401).body(false);
                    }
                }
            }
        }

        if (jwtAccess == null) {
            log.warn("No se encontró la cookie de acceso esperada");
            return ResponseEntity.status(401).body(false);
        }

        try {
            if (!tokenProvider.validateToken(jwtAccess)) {
                log.warn("Token inválido o revocado en /expira1");
                return ResponseEntity.status(401).body(false);
            }

            Claims claims = tokenProvider.parseToken(jwtAccess);
            long expTimestamp = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            boolean porExpirar = (expTimestamp - now) <= 30_000;

            log.info("Token expira en {} ms → ¿por expirar?: {}", (expTimestamp - now), porExpirar);
            return ResponseEntity.ok(porExpirar);

        } catch (Exception e) {
            log.error("Error al procesar el token en /expira1: {}", e.getMessage());
            return ResponseEntity.status(401).body(false);
        }
    }

}
