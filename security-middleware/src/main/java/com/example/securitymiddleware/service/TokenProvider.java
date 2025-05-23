package com.example.securitymiddleware.service;

import com.example.securitymiddleware.config.JwtProperties;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.repository.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProvider {

    private final TokenRepository tokenRepository;
    private final JwtProperties jwtProps;

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            validateClaims(claims);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("Claims vacíos o nulos: {}", e.getMessage());
            throw new TokenValidationException("Claims vacíos o nulos");
        } catch (JwtException e) {
            handleJwtException(e);
            return false;
        } catch (Exception e) {
            log.error("Error desconocido al validar token: {}", e.getMessage());
            throw new TokenValidationException("Error desconocido al validar token");
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateClaims(Claims claims) {
        String jti = claims.getId();
        if (tokenRepository.isTokenBlacklisted(jti)) {
            log.warn("Token revocado (jti={})", jti);
            throw new TokenValidationException("Token revocado (blacklist)");
        }
        log.info("Token válido");
    }

    private void handleJwtException(JwtException e) {
        String mensaje;
        if (e instanceof ExpiredJwtException) mensaje = "Token expirado";
        else if (e instanceof SignatureException) mensaje = "Firma inválida";
        else if (e instanceof MalformedJwtException) mensaje = "Token mal formado";
        else if (e instanceof UnsupportedJwtException) mensaje = "Token no soportado";
        else mensaje = "Error JWT";

        log.error("{}: {}", mensaje, e.getMessage());
        throw new TokenValidationException(mensaje);
    }

    public String generateSessionToken(String idUsuario, String idAplicacion, String idsession) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "sesion");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        return generateToken(idUsuario, jwtProps.getSessionExpiration(), claims);
    }

    public String generateRefreshToken(String idUsuario, String idAplicacion, String idsession, int maxRefrescos) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "refresco");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        claims.put("MaxRefrescos", maxRefrescos);
        return generateToken(idUsuario, jwtProps.getRefreshExpiration(), claims);
    }

    public String generateAccessToken(String idUsuario, String idAplicacion, String idsession, List<String> roles, int numeroRefresco) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "acceso");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        claims.put("roles", roles);
        claims.put("NumeroRefresco", numeroRefresco);
        return generateToken(idUsuario, jwtProps.getAccessExpiration(), claims);
    }

    public String generateAccessToken(String idUsuario, String idAplicacion, String idsession, List<String> roles, int numeroRefresco, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "acceso");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        claims.put("roles", roles);
        claims.put("NumeroRefresco", numeroRefresco);

        // Add additional claims if provided
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }

        return generateToken(idUsuario, jwtProps.getAccessExpiration(), claims);
    }

    public String generateTemporalToken(String subject, long expirationMs, Map<String, Object> claims) {
        return generateToken(subject, expirationMs, claims);
    }

    public String generateToken(String subject, long expirationMs, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        claims.putIfAbsent("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .issuer(jwtProps.getIssuer())
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .claims(claims)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProps.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String getJtiFromToken(String token) {
        try {
            return parseToken(token).getId();
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado, extrayendo jti de claims caducados");
            return e.getClaims().getId();
        } catch (Exception e) {
            log.error("Error extrayendo jti: {}", e.getMessage());
            return null;
        }
    }

}
