package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.repository.TokenRepository;
import com.example.securitymiddleware.service.SecurityInfoService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@Slf4j
@RestController
public class LogInEndController extends BaseController {

    @Value("${jwt.maxRefresh}")
    private int maxRefresh;

    private final SecurityInfoService securityInfoService;
    private final TokenRepository tokenRepository;
    private final EncryptionService encryptionService;

    public LogInEndController(TokenProvider tokenProvider,
                              SecurityInfoService securityInfoService,
                              TokenRepository tokenRepository,
                              EncryptionService encryptionService) {
        super(tokenProvider);
        this.securityInfoService = securityInfoService;
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/login2End")
    public ResponseEntity<?> login2End(HttpServletRequest request) {
        String sessiontmp = getCookieValue(request, SESSIONTMP);
        String certHeader = request.getHeader(XCERTAUTH);
        String clientIp = request.getRemoteAddr();

        log.info("Completando login (Opción 2): IP={}", clientIp);

        if (sessiontmp == null || sessiontmp.isBlank()) {
            log.warn("Falta la cookie Sessiontmp: IP={}", clientIp);
            throw new CookieNotFoundException("Falta la cookie Sessiontmp");
        }
        if (certHeader == null || certHeader.isBlank()) {
            log.warn("Falta la cabecera X-Cert-Auth: IP={}", clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }

        if (!tokenProvider.validateToken(sessiontmp)) {
            log.warn("Token temporal inválido: IP={}", clientIp);
            throw new TokenValidationException("Token de sesión temporal inválido");
        }

        Claims claims = tokenProvider.parseToken(sessiontmp);
        String idusuario = claims.getSubject();
        String idaplicacion = claims.get("idaplicacion", String.class);
        String idsession = claims.get("idsession", String.class);

        log.info("Token temporal válido: usuario={}, aplicacion={}, sesion={}, IP={}", idusuario, idaplicacion, idsession, clientIp);

        // Generar fingerprint y hash
        String fingerprint = UUID.randomUUID().toString();
        String hashFingerprint = sha256Hex(fingerprint);

        // Generar tokens
        String jwtSesion = tokenProvider.generateSessionToken(idusuario, idaplicacion, idsession);
        String jwtRefresh = tokenProvider.generateRefreshToken(idusuario, idaplicacion, idsession, maxRefresh);
        List<String> roles = securityInfoService.getRolesForUser(idusuario, idaplicacion);

        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("hashFingerprint", hashFingerprint);
        String jwtAcceso = tokenProvider.generateAccessToken(idusuario, idaplicacion, idsession, roles, 0, additionalClaims);

        tokenRepository.storeAccessToken(idsession, jwtAcceso);
        tokenRepository.storeRefreshToken(idsession, jwtRefresh);

        // Encriptar contenido de cookie de sesión
        String sessionPlain = jwtSesion + "::" + jwtRefresh;
        String sessionEncrypted = encryptionService.encrypt(sessionPlain);

        // Encriptar token de acceso para cabecera Authorization
        String jwtAccesoEncrypted = encryptionService.encrypt(jwtAcceso);

        // Cookies
        ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE + idsession, sessionEncrypted)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .build();

        ResponseCookie protectionCookie = ResponseCookie.from(PROTECTION_COOKIE + idsession,
                        Base64.getEncoder().encodeToString(fingerprint.getBytes(StandardCharsets.UTF_8)))
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .build();

        ResponseCookie deleteTmp = ResponseCookie.from(SESSIONTMP, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        log.info("Login completado: usuario={}, sesion={}, IP={}", idusuario, idsession, clientIp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(SET_COOKIE, sessionCookie.toString())
                .header(SET_COOKIE, protectionCookie.toString())
                .header(SET_COOKIE, deleteTmp.toString())
                .header(AUTHORIZATION, jwtAccesoEncrypted)
                .header(XIDSESSION, idsession)
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
