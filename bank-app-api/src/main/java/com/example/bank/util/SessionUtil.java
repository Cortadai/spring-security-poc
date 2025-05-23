package com.example.bank.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import static com.example.bank.constants.ApplicationConstants.XIDSESSION;

public final class SessionUtil {

    private SessionUtil() {
        // Private constructor to prevent instantiation
    }

    public static String getSessionId(HttpServletRequest request) {
        return request.getHeader(XIDSESSION);
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}