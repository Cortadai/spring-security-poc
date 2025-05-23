package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.EncryptionService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ValidateTokenController extends BaseController {

    private final EncryptionService encryptionService;

    public ValidateTokenController(TokenProvider tokenProvider, EncryptionService encryptionService) {
        super(tokenProvider);
        this.encryptionService = encryptionService;
    }

    @PostMapping("/validartoken")
    public ResponseEntity<Map<String, Object>> validarTokenV2(@RequestBody Map<String, String> body) {
        String encryptedToken = body.get("token");
        String idsession = body.get("idsession");
        String fingerprint = body.get("fingerprint");

        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new ParameterNotFoundException("Falta el campo 'token'");
        }
        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta el campo 'idsession'");
        }
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new ParameterNotFoundException("Falta el campo 'fingerprint'");
        }

        String token;
        try {
            token = encryptionService.decrypt(encryptedToken);
        } catch (Exception e) {
            throw new TokenValidationException("Error desencriptando token");
        }

        if (!tokenProvider.validateToken(token)) {
            throw new TokenValidationException("Token inválido o expirado");
        }

        Claims claims = tokenProvider.parseToken(token);

        // Verificar fingerprint
        String expectedHash = calcularHashFingerprint(fingerprint);
        String claimHash = claims.get("hashFingerprint", String.class);
        if (claimHash == null || !expectedHash.equals(claimHash)) {
            throw new TokenValidationException("Fingerprint no válido");
        }

        // Extraer datos
        String tipo = claims.get("typ", String.class);
        String idusuario = claims.get("idusuario", String.class);
        String idaplicacion = claims.get("idaplicacion", String.class);
        List<String> roles = claims.get("roles", List.class);

        Map<String, Object> result = new HashMap<>();
        result.put("valido", true);
        result.put("typ", tipo);
        result.put("idusuario", idusuario);
        result.put("idaplicacion", idaplicacion);
        result.put("idsession", idsession);
        result.put("roles", roles);
        result.put("exp", claims.getExpiration().getTime());

        if ("acceso".equals(tipo)) {
            result.put("NumeroRefresco", claims.get("NumeroRefresco", Integer.class));
        }

        log.info("Token validado (opción 2): usuario={}, sesion={}", idusuario, idsession);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    private String calcularHashFingerprint(String fingerprint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando hash del fingerprint", e);
        }
    }
}
