package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.service.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RestController
@Slf4j
public class LogInBeginController extends BaseController {

    @Value("${jwt.temporalExpiration}")
    private long jwtTemporalExpirationMs;

    public LogInBeginController(TokenProvider tokenProvider) {
        super(tokenProvider);
    }

    @GetMapping("/loginBegin")
    public ResponseEntity<?> loginBegin(
            @RequestParam("idaplicacion") String idaplicacion,
            @CookieValue(name = "COOKIESSO", required = false) String cookiesso,
            @RequestHeader(value = XCERTAUTH, required = false) String cert,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();

        log.info("Inicio de proceso de login: idaplicacion={}, IP={}", 
                 idaplicacion, clientIp);

        if (cookiesso == null || cookiesso.isBlank()) {
            log.warn("Intento de login sin cookie SSO: idaplicacion={}, IP={}", 
                     idaplicacion, clientIp);
            throw new ParameterNotFoundException("Falta la cookie COOKIESSO");
        }
        if (idaplicacion == null || idaplicacion.isBlank()) {
            log.warn("Intento de login sin idaplicacion: IP={}", clientIp);
            throw new ParameterNotFoundException("Falta el parámetro idaplicacion");
        }
        if (cert == null || cert.isBlank()) {
            log.warn("Intento de login sin certificado: idaplicacion={}, IP={}", 
                     idaplicacion, clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }

        Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("idaplicacion", idaplicacion);
        String idsession = UUID.randomUUID().toString();
        tokenClaims.put("idsession", idsession);

        log.debug("Generando token temporal para: idaplicacion={}, idsession={}, IP={}", 
                 idaplicacion, idsession, clientIp);

        String jwtTemporal = tokenProvider.generateTemporalToken(cookiesso, jwtTemporalExpirationMs, tokenClaims);

        ResponseCookie sessiontmp = ResponseCookie.from(SESSIONTMP, jwtTemporal)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SAME_SITE)
                .maxAge(30)
                .build();

        log.info("Login iniciado correctamente: idaplicacion={}, idsession={}, IP={}", 
                 idaplicacion, idsession, clientIp);

        return ResponseEntity
                .accepted()
                .header(SET_COOKIE, sessiontmp.toString())
                .build();
    }

}
