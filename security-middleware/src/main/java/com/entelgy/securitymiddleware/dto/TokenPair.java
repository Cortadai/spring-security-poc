package com.entelgy.securitymiddleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {

    private String accessToken;
    private String refreshToken;

}
