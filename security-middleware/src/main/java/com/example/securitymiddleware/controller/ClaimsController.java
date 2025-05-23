package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;

@Slf4j
@RestController
public class ClaimsController extends BaseController {

    private final EncryptionService encryptionService;

    public ClaimsController(TokenProvider tokenProvider, EncryptionService encryptionService) {
        super(tokenProvider);
        this.encryptionService = encryptionService;
    }

    @GetMapping("/obtenerclaims2")
    public Map<String, Object> obtenerClaims(
            HttpServletRequest request,
            @RequestHeader(value = XIDSESSION, required = false) String idsession,
            @RequestHeader(value = XCERTAUTH, required = false) String certAuth,
            @RequestHeader(value = AUTHORIZATION, required = false) String authorization
    ) {
        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }
        if (certAuth == null || certAuth.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }
        if (authorization == null || authorization.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera Authorization");
        }

        String sessionCookieValue = getCookieValue(request, SESSION_COOKIE + idsession);
        String protectionCookieValue = getCookieValue(request, PROTECTION_COOKIE + idsession);

        if (sessionCookieValue == null || protectionCookieValue == null) {
            throw new CookieNotFoundException("Faltan cookies de sesión o protección");
        }

        // Desencriptar contenido de la cookie de sesión
        String decryptedSession = encryptionService.decrypt(sessionCookieValue);
        String[] sessionParts = decryptedSession.split("::");
        if (sessionParts.length < 1) {
            throw new RuntimeException("La cookie de sesión está mal formada");
        }

        String jwtSession = sessionParts[0];
        String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;

        // Desencriptar token de acceso
        String jwtAccess = encryptionService.decrypt(authorization);

        // Decodificar fingerprint
        String fingerprint = new String(java.util.Base64.getDecoder().decode(protectionCookieValue), StandardCharsets.UTF_8);

        // Validar tokens
        Claims sessionClaims = parseValidToken(jwtSession, "Token de sesión inválido");
        Claims refreshClaims = jwtRefresh != null ? parseValidToken(jwtRefresh, "Token de refresco inválido") : null;
        Claims accessClaims = parseValidToken(jwtAccess, "Token de acceso inválido");

        // Verificar fingerprint
        verificarFingerprint(fingerprint, accessClaims);

        // Devolver los claims
        Map<String, Object> result = new HashMap<>();
        result.put("session", sessionClaims);
        result.put("refresh", refreshClaims);
        result.put("access", accessClaims);
        return result;
    }

    private void verificarFingerprint(String fingerprint, Claims accessClaims) {
        String hashFingerprint = calcularHashFingerprint(fingerprint);
        String claimHashFingerprint = accessClaims.get("hashFingerprint", String.class);

        if (claimHashFingerprint == null) {
            throw new TokenValidationException("El token no contiene el claim hashFingerprint");
        }

        if (!hashFingerprint.equals(claimHashFingerprint)) {
            throw new TokenValidationException("Fingerprint inválido");
        }
    }

    private String calcularHashFingerprint(String fingerprint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error al generar hash del fingerprint: {}", e.getMessage());
            throw new RuntimeException("Error al generar hash del fingerprint", e);
        }
    }
}
