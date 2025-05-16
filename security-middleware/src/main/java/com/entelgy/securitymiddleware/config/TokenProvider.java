package com.entelgy.securitymiddleware.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class TokenProvider {

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
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            log.info("Token is valid");
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        log.info("Token is invalid");
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
        JwtBuilder builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .claims(claims)
                .expiration(expiryDate)
                .signWith(getSigningKey());
        return builder.compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

}
