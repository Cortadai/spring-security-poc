package com.entelgy.securitymiddleware.config;

import com.entelgy.securitymiddleware.repository.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    private final TokenRepository tokenRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.sessionExpiration}")
    private long jwtSessionExpirationMs;

    @Value("${jwt.accessExpiration}")
    private long jwtAccessExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationMs;

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);

            String jti = claims.getId(); // Extrae el 'jti'
            if (tokenRepository.isTokenBlacklisted(jti)) {
                log.warn("Token con jti={} está revocado (blacklist)", jti);
                return false;
            }

            log.info("Token válido");
            return true;

        } catch (ExpiredJwtException e) {
            log.error("Token expirado: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Firma JWT inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT mal formado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims del JWT vacíos o nulos: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error desconocido al validar token: {}", e.getMessage());
        }

        return false;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateSessionToken(String idUsuario, String idAplicacion, String idsession) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "sesion");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        return generateToken(idUsuario, jwtSessionExpirationMs, claims);
    }

    public String generateRefreshToken(String idUsuario, String idAplicacion, String idsession, int maxRefrescos) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "refresco");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        claims.put("MaxRefrescos", maxRefrescos);
        return generateToken(idUsuario, jwtRefreshExpirationMs, claims);
    }

    public String generateAccessToken(String idUsuario, String idAplicacion, String idsession, List<String> roles, int numeroRefresco) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "acceso");
        claims.put("idusuario", idUsuario);
        claims.put("idaplicacion", idAplicacion);
        claims.put("idsession", idsession);
        claims.put("roles", roles);
        claims.put("NumeroRefresco", numeroRefresco);
        return generateToken(idUsuario, jwtAccessExpirationMs, claims);
    }

    public String generateTemporalToken(String subject, long expirationMs, Map<String, Object> claims) {
        return generateToken(subject, expirationMs, claims);
    }

    private String generateToken(String subject, long expirationMs, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // Añadir jti único
        claims.putIfAbsent("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .claims(claims)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getJtiFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado al extraer jti, usando claims del token caducado");
            return e.getClaims().getId();
        } catch (Exception e) {
            log.error("No se pudo extraer el jti del token: {}", e.getMessage());
            return null;
        }
    }
}
