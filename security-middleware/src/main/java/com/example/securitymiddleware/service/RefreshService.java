package com.example.securitymiddleware.service;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.TokenParseException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.example.securitymiddleware.constants.ApplicationConstants.PROTECTION_COOKIE;
import static com.example.securitymiddleware.constants.ApplicationConstants.SESSION_COOKIE;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshService {

    private final TokenProvider tokenProvider;
    private final SecurityInfoService securityInfoService;
    private final TokenRepository tokenRepository;
    private final EncryptionService encryptionService;

    public String procesarRefresco(HttpServletRequest request, HttpServletResponse response, String idsession, String certAuth, String jwtAccessEncrypted) {
        String clientIp = request.getRemoteAddr();
        log.debug("Iniciando procesamiento de refresco (Opción 2): sesion={}, IP={}", idsession, clientIp);

        // Obtener cookies
        String sessionCookie = getCookieValue(request, SESSION_COOKIE + idsession);
        String protectionCookie = getCookieValue(request, PROTECTION_COOKIE + idsession);

        // Desencriptar y separar tokens de la cookie de sesión
        String decryptedSession = encryptionService.decrypt(sessionCookie);
        String[] sessionParts = decryptedSession.split("::");
        if (sessionParts.length < 2) {
            throw new TokenParseException("La cookie de sesión no contiene los dos tokens esperados");
        }
        String jwtSession = sessionParts[0];
        String jwtRefresh = sessionParts[1];

        // Desencriptar token de acceso
        String jwtAccess = encryptionService.decrypt(jwtAccessEncrypted);

        // Decodificar fingerprint
        String fingerprint = new String(Base64.getDecoder().decode(protectionCookie), StandardCharsets.UTF_8);

        // Validar tokens
        Claims sessionClaims = parseValidToken(jwtSession, "Token de sesión inválido");
        Claims refreshClaims = parseValidToken(jwtRefresh, "Token de refresco inválido");
        Claims accessClaims = parseValidToken(jwtAccess, "Token de acceso inválido");

        // Verificar fingerprint
        verificarFingerprint(fingerprint, accessClaims);
        log.debug("Fingerprint verificado correctamente: sesion={}", idsession);

        // Verificar número de refrescos
        int actualRefresco = accessClaims.get("NumeroRefresco", Integer.class);
        int maxRefrescos = refreshClaims.get("MaxRefrescos", Integer.class);

        if (actualRefresco >= maxRefrescos) {
            throw new TokenValidationException("Se alcanzó el número máximo de refrescos permitidos");
        }

        String idUsuario = sessionClaims.get("idusuario", String.class);
        String idAplicacion = sessionClaims.get("idaplicacion", String.class);

        // Revocar token de acceso anterior
        revocarTokenSiAplica(accessClaims);

        // Generar nuevo token de acceso con mismo fingerprint
        List<String> roles = securityInfoService.getRolesForUser(idUsuario, idAplicacion);
        String hashFingerprint = calcularHashFingerprint(fingerprint);

        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("hashFingerprint", hashFingerprint);

        String nuevoJwtAccess = tokenProvider.generateAccessToken(idUsuario, idAplicacion, idsession, roles, actualRefresco + 1, additionalClaims);
        tokenRepository.storeAccessToken(idsession, nuevoJwtAccess);
        String jwtAccessEncryptedNuevo = encryptionService.encrypt(nuevoJwtAccess);

        if (actualRefresco + 1 == maxRefrescos) {
            revocarTokenSiAplica(refreshClaims);
        }

        log.info("Nuevo token de acceso emitido: sesion={}, usuario={}, refresco={}, IP={}",
                idsession, idUsuario, actualRefresco + 1, clientIp);

        return jwtAccessEncryptedNuevo;
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
            throw new RuntimeException("Error al generar hash del fingerprint", e);
        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new CookieNotFoundException("No se encontró la cookie " + name));
    }

    private Claims parseValidToken(String jwt, String errorMessage) {
        if (!tokenProvider.validateToken(jwt)) throw new TokenValidationException(errorMessage);
        return tokenProvider.parseToken(jwt);
    }

    private void revocarTokenSiAplica(Claims claims) {
        String jti = claims.getId();
        Date exp = claims.getExpiration();
        long ttl = exp.getTime() - System.currentTimeMillis();
        if (jti != null && ttl > 0) {
            tokenRepository.blacklistToken(jti, ttl);
            log.info("Token revocado con jti={} (TTL: {}ms)", jti, ttl);
        }
    }
}
