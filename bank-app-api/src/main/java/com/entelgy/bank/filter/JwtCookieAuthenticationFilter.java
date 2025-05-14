package com.entelgy.bank.filter;

import com.entelgy.bank.config.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter implements Filter {

    private final TokenProvider tokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. Buscar la cookie Session-{idsession}
        String token = null;
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (cookie.getName().startsWith("Session-")) {
                    try {
                        byte[] decoded = java.util.Base64.getDecoder().decode(cookie.getValue());
                        String jwtConcat = new String(decoded);
                        String[] parts = jwtConcat.split("::");
                        token = parts[0]; // solo usamos el token de sesión
                        break;
                    } catch (Exception e) {
                        log.warn("Error decodificando cookie JWT: {}", e.getMessage());
                    }
                }
            }
        }

        // 2. Validar y procesar el token
        if (token != null && tokenProvider.validateToken(token)) {
            Claims claims = tokenProvider.parseToken(token);
            String username = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}