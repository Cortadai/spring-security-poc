package com.entelgy.securitymiddleware.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

import static com.entelgy.securitymiddleware.constants.ApplicationConstants.*;

@Repository
@RequiredArgsConstructor
public class TokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.accessExpiration}")
    private long accessExpiration;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;

    // Guardar tokens por idsession
    public void storeAccessToken(String idsession, String accessToken) {
        redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + idsession, accessToken);
        redisTemplate.expire(ACCESS_TOKEN_PREFIX + idsession, accessExpiration, TimeUnit.MILLISECONDS);
    }

    public void storeRefreshToken(String idsession, String refreshToken) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + idsession, refreshToken);
        redisTemplate.expire(REFRESH_TOKEN_PREFIX + idsession, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    // Blacklist por jti
    public void blacklistToken(String jti, long expirationMs) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked");
        redisTemplate.expire(BLACKLIST_PREFIX + jti, expirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String jti) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
    }

    // Eliminación y revocación al hacer logout
    public void removeTokens(String idsession, String accessJti, String refreshJti) {
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + idsession);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + idsession);

        if (accessJti != null) {
            blacklistToken(accessJti, accessExpiration);
        }

        if (refreshJti != null) {
            blacklistToken(refreshJti, refreshExpiration);
        }
    }

}
