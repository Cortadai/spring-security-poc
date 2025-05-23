package com.example.bank.config;

import com.example.bank.dto.TokenPair;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.bank.constants.ApplicationConstants.JWT_SUBJECT;

@Slf4j
@Component
public class TokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpirationMs;

    public TokenPair generateTokenPair(Authentication authentication) {
        log.info("Begin of generateTokenPair");
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }

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

    public String generateAccessToken(Authentication authentication) {
        Map<String, Object> accessTokenClaims = new HashMap<>();
        accessTokenClaims.put("username", authentication.getName());
        accessTokenClaims.put("authorities", authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")));
        log.info("Access Token Generated");
        return generateToken(JWT_SUBJECT, jwtExpirationMs, accessTokenClaims);
    }

    private String generateRefreshToken(Authentication authentication) {
        String subject = authentication.getName();
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "refresh");
        log.info("Refresh Token Generated");
        return generateToken(subject, refreshExpirationMs, claims);
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

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

}
