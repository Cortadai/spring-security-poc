package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.repository.TokenRepository;
import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@Slf4j
@RestController
public class LogOffController extends BaseController {

    private final TokenRepository tokenRepository;
    private final EncryptionService encryptionService;

    public LogOffController(TokenProvider tokenProvider, TokenRepository tokenRepository, EncryptionService encryptionService) {
        super(tokenProvider);
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/logoff2")
    public ResponseEntity<?> logoff2(
            HttpServletRequest request,
            @RequestHeader(value = XCERTAUTH, required = false) String certHeader,
            @RequestHeader(value = XIDSESSION, required = false) String idsession,
            @RequestHeader(value = AUTHORIZATION, required = false) String authorization
    ) {
        String clientIp = request.getRemoteAddr();
        log.info("Iniciando logout (Opción 2): sesion={}, IP={}", idsession, clientIp);

        if (certHeader == null || certHeader.isBlank())
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        if (idsession == null || idsession.isBlank())
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        if (authorization == null || authorization.isBlank())
            throw new ParameterNotFoundException("Falta la cabecera Authorization");

        String sessionCookieName = SESSION_COOKIE + idsession;
        String protectionCookieName = PROTECTION_COOKIE + idsession;

        String sessionCookieValue = getCookieValue(request, sessionCookieName);
        String protectionCookieValue = getCookieValue(request, protectionCookieName);

        if (sessionCookieValue == null || protectionCookieValue == null) {
            throw new CookieNotFoundException("Faltan cookies de sesión o protección para idsession=" + idsession);
        }

        try {
            // Desencriptar cookie de sesión
            String decryptedSession = encryptionService.decrypt(sessionCookieValue);
            String[] sessionParts = decryptedSession.split("::");
            String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;

            // Desencriptar token de acceso
            String decryptedAccessToken = encryptionService.decrypt(authorization);

            // Validar fingerprint
            String fingerprint = new String(Base64.getDecoder().decode(protectionCookieValue), StandardCharsets.UTF_8);
            String hash = sha256Hex(fingerprint);
            Claims accessClaims = tokenProvider.parseToken(decryptedAccessToken);
            String claimHash = accessClaims.get("hashFingerprint", String.class);
            if (!hash.equals(claimHash)) {
                throw new SecurityException("Fingerprint no válido");
            }

            // Validar y revocar tokens
            String jtiAccess = tokenProvider.getJtiFromToken(decryptedAccessToken);
            String jtiRefresh = jwtRefresh != null ? tokenProvider.getJtiFromToken(jwtRefresh) : null;

            tokenRepository.removeTokens(idsession, jtiAccess, jtiRefresh);

            log.info("Logout completado: sesion={}, jtiAccess={}, jtiRefresh={}, IP={}",
                    idsession, jtiAccess, jtiRefresh, clientIp);

        } catch (Exception e) {
            log.warn("Error en logout (Opción 2): sesion={}, error={}", idsession, e.getMessage());
            throw new RuntimeException("Error durante logoff (Opción 2)", e);
        }

        // Borrar cookies
        ResponseCookie deleteSession = ResponseCookie.from(sessionCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteProtection = ResponseCookie.from(protectionCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(SET_COOKIE, deleteSession.toString())
                .header(SET_COOKIE, deleteProtection.toString())
                .header(XIDSESSION, "")
                .header(AUTHORIZATION, "")
                .build();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No se pudo generar hash SHA-256", e);
        }
    }
}
