package com.example.securitymiddleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private String issuer;
    private long sessionExpiration;
    private long accessExpiration;
    private long refreshExpiration;

}