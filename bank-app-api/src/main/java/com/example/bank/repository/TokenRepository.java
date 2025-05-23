package com.example.bank.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class TokenRepository {

    private final RedisTemplate redisTemplate;

    // key prefixes for token storage
    private static final String ACCESS_TOKEN_PREFIX = "user:access:";
    private static final String REFRESH_TOKEN_PREFIX = "user:refresh:";

    // key prefixes for token blacklist
    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_BLACKLIST_PREFIX = "blacklist:refresh:";

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpirationMs;

    // Store both tokens for a user
    public void storeTokens(String username, String accessToken, String refreshToken) {
        String accessKey = ACCESS_TOKEN_PREFIX + username;
        storeToken(accessKey, accessToken, jwtExpirationMs);

        String refreshKey = REFRESH_TOKEN_PREFIX + username;
        storeToken(refreshKey, refreshToken, refreshExpirationMs);
    }

    // Retrieve access token for a user
    public String getAccessToken(String username) {
        String accessKey = ACCESS_TOKEN_PREFIX + username;
        return getToken(accessKey);
    }

    // Retrieve refresh token for a user
    public String getRefreshToken(String username) {
        String accessKey = REFRESH_TOKEN_PREFIX + username;
        return getToken(accessKey);
    }

    // Remove all tokens for a user (complete logout)
    public void removeAllTokens(String username) {
        String accessToken = getAccessToken(username);
        String refreshToken = getRefreshToken(username);

        String accessKey = ACCESS_TOKEN_PREFIX + username;
        String refreshKey = REFRESH_TOKEN_PREFIX + username;

        redisTemplate.delete(accessKey);
        redisTemplate.delete(refreshKey);

        if(accessToken != null) {
            String accessBlacklistKey = ACCESS_BLACKLIST_PREFIX + accessToken;
            blacklistToken(accessBlacklistKey, jwtExpirationMs);
        }

        if(refreshToken != null) {
            String refreshBlacklistKey = REFRESH_BLACKLIST_PREFIX + refreshToken;
            blacklistToken(refreshBlacklistKey, refreshExpirationMs);
        }
    }

    public boolean isAccessTokenBlacklisted(String accessToken) {
        String key = ACCESS_BLACKLIST_PREFIX + accessToken;
        return redisTemplate.hasKey(key);
    }

    public boolean isRefreshTokenBlacklisted(String refreshToken) {
        String key = REFRESH_BLACKLIST_PREFIX + refreshToken;
        return redisTemplate.hasKey(key);
    }

    public void removeAccessToken(String username) {
        String accessToken = getAccessToken(username);
        String accessKey = ACCESS_TOKEN_PREFIX + username;
        redisTemplate.delete(accessKey);
        String accessBlacklistKey = ACCESS_BLACKLIST_PREFIX + accessToken;
        blacklistToken(accessBlacklistKey, jwtExpirationMs);
    }

    private void storeToken(String key, String token, long expiration){
        redisTemplate.opsForValue().set(key, token);
        redisTemplate.expire(key, expiration, TimeUnit.MILLISECONDS);
    }

    private String getToken(String accessKey) {
        Object token = redisTemplate.opsForValue().get(accessKey);
        return token != null ? token.toString() : null;
    }

    private void blacklistToken(String blacklistKey, long expiration) {
        redisTemplate.opsForValue().set(blacklistKey, "blacklisted");
        redisTemplate.expire(blacklistKey, expiration, TimeUnit.MILLISECONDS);
    }


}
