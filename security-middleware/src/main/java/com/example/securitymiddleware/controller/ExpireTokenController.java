package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.securitymiddleware.constants.ApplicationConstants.AUTHORIZATION;
import static com.example.securitymiddleware.constants.ApplicationConstants.XIDSESSION;

@Slf4j
@RestController
public class ExpireTokenController extends BaseController {

    private final EncryptionService encryptionService;

    public ExpireTokenController(TokenProvider tokenProvider, EncryptionService encryptionService) {
        super(tokenProvider);
        this.encryptionService = encryptionService;
    }

    @GetMapping("/expira2")
    public ResponseEntity<Boolean> tokenPorExpirarV2(HttpServletRequest request) {
        String idsession = request.getHeader(XIDSESSION);
        String authorization = request.getHeader(AUTHORIZATION);

        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }
        if (authorization == null || authorization.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera Authorization");
        }

        String jwtAccess = encryptionService.decrypt(authorization);

        if (!tokenProvider.validateToken(jwtAccess)) {
            throw new TokenValidationException("Token inválido o revocado");
        }

        Claims claims = tokenProvider.parseToken(jwtAccess);
        long expTimestamp = claims.getExpiration().getTime();
        long now = System.currentTimeMillis();
        boolean porExpirar = (expTimestamp - now) <= 30_000;

        log.info("Token expira en {} ms → ¿por expirar?: {}", (expTimestamp - now), porExpirar);
        return ResponseEntity.ok(porExpirar);
    }

}
