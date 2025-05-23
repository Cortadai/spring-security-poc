package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.service.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;

@Slf4j
@RestController
public class SessionController extends BaseController {

    private final EncryptionService encryptionService;

    public SessionController(TokenProvider tokenProvider, EncryptionService encryptionService) {
        super(tokenProvider);
        this.encryptionService = encryptionService;
    }

    @GetMapping("/estadosession")
    public ResponseEntity<?> estadoSesion(HttpServletRequest request) {
        String idsession = request.getHeader(XIDSESSION);
        String certHeader = request.getHeader(XCERTAUTH);

        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }
        if (certHeader == null || certHeader.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }

        String sessionCookieName = SESSION_COOKIE + idsession;
        String sessionCookieValue = getCookieValue(request, sessionCookieName);
        if (sessionCookieValue == null) {
            throw new CookieNotFoundException("No se encontró la cookie de sesión " + sessionCookieName);
        }

        // ✅ Desencriptar correctamente
        String decrypted = encryptionService.decrypt(sessionCookieValue);
        String[] parts = decrypted.split("::");
        if (parts.length < 2) {
            throw new TokenValidationException("Cookie de sesión mal formada (faltan tokens)");
        }

        String jwtSession = parts[0];
        String jwtRefresh = parts[1];

        if (!tokenProvider.validateToken(jwtSession)) {
            throw new TokenValidationException("Token de sesión inválido");
        }

        if (!tokenProvider.validateToken(jwtRefresh)) {
            throw new TokenValidationException("Token de refresco inválido o revocado");
        }

        log.info("Sesión activa: idsession={}, IP={}", idsession, request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

}
