package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.securitymiddleware.constants.ApplicationConstants.ACCESS_COOKIE;
import static com.example.securitymiddleware.constants.ApplicationConstants.XIDSESSION;

@Slf4j
@RestController
public class ExpireTokenController extends BaseController {

    public ExpireTokenController(TokenProvider tokenProvider) {
        super(tokenProvider);
    }

    @GetMapping("/expira1")
    public ResponseEntity<Boolean> tokenPorExpirar(HttpServletRequest request) {
        String idsession = request.getHeader(XIDSESSION);
        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }

        String cookieName = ACCESS_COOKIE + idsession;
        String encodedToken = getCookieValue(request, cookieName);
        if (encodedToken == null) {
            throw new CookieNotFoundException("Falta la cookie " + cookieName);
        }

        String jwtAccess = decodeBase64(encodedToken);

        if (!tokenProvider.validateToken(jwtAccess)) {
            throw new TokenValidationException("Token inválido o revocado");
        }

        Claims claims = tokenProvider.parseToken(jwtAccess);
        long expTimestamp = claims.getExpiration().getTime();
        long now = System.currentTimeMillis();
        boolean porExpirar = (expTimestamp - now) <= 30_000;    // 30 segundos antes de expirar

        log.info("Token expira en {} ms → ¿por expirar?: {}", (expTimestamp - now), porExpirar);
        return ResponseEntity.ok(porExpirar);
    }
}
