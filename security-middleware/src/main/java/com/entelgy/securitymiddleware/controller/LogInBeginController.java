package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LogInBeginController {

    @Value("${jwt.temporalExpiration}")
    private long jwtTemporalExpirationMs;

    private final TokenProvider tokenProvider;

    @GetMapping("/loginBegin")
    public void loginBegin(
            @RequestParam("idaplicacion") String idaplicacion,
            @CookieValue(name = "COOKIESSO", required = false) String cookiesso,
            @RequestHeader(value = "X-Cert-Auth", required = false) String cert,
            HttpServletResponse response
    ) {
        // Validación mínima de parámetros
        if (cookiesso == null || cookiesso.isBlank()
                || idaplicacion == null || idaplicacion.isBlank()
                || cert == null || cert.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Generar JWT temporal
        Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("idaplicacion", idaplicacion);
        tokenClaims.put("idsession", UUID.randomUUID().toString());

        String jwtTemporal = tokenProvider.generateTemporalToken(cookiesso, jwtTemporalExpirationMs, tokenClaims);

        // Crear cookie con SameSite=Strict y atributos seguros
        ResponseCookie sessiontmp = ResponseCookie.from("Sessiontmp", jwtTemporal)
                .httpOnly(true)
                .secure(false) // pon true en producción con HTTPS
                .path("/")
                .sameSite("Strict")
                .maxAge(30)
                .build();

        // Añadir la cookie al header Set-Cookie
        response.addHeader("Set-Cookie", sessiontmp.toString());

        // Enviar código HTTP 202 Accepted
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }
}
