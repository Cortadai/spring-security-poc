package com.example.securitymiddleware.repository;

import com.example.securitymiddleware.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;

@Repository
@RequiredArgsConstructor
public class TokenRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProps;

    public void storeAccessToken(String idsession, String token) {
        storeToken(ACCESS_TOKEN_PREFIX + idsession, token, jwtProps.getAccessExpiration());
    }

    public void storeRefreshToken(String idsession, String token) {
        storeToken(REFRESH_TOKEN_PREFIX + idsession, token, jwtProps.getRefreshExpiration());
    }

    private void storeToken(String key, String token, long ttlMs) {
        redisTemplate.opsForValue().set(key, token);
        redisTemplate.expire(key, ttlMs, TimeUnit.MILLISECONDS);
    }

    public void blacklistToken(String jti, long expirationMs) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked");
        redisTemplate.expire(BLACKLIST_PREFIX + jti, expirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public void removeTokens(String idsession, String accessJti, String refreshJti) {
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + idsession);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + idsession);

        if (accessJti != null) blacklistToken(accessJti, jwtProps.getAccessExpiration());
        if (refreshJti != null) blacklistToken(refreshJti, jwtProps.getRefreshExpiration());
    }

}

