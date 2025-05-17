package com.entelgy.bank.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.entelgy.bank.constants.ApplicationConstants.ACCESS_COOKIE;
import static com.entelgy.bank.constants.ApplicationConstants.MIDDLEWARE_URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 1. Buscar la cookie Session-{idsession}
        String token = null;
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (cookie.getName().startsWith(ACCESS_COOKIE)) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(cookie.getValue());
                        token = new String(decoded);
                        break;
                    } catch (Exception e) {
                        log.warn("Error decodificando cookie JWT de acceso: {}", e.getMessage());
                    }
                }
            }
        }
        // 2. Validar y procesar el token
        if (token != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("vamos a validar este token: " + token.substring(token.length() - 10));
            Map<String, String> body = Map.of("token", token);
            HttpEntity<Map<String, String>> validationRequest = new HttpEntity<>(body, headers);

            ResponseEntity<Map> validationResponse = restTemplate
                    .postForEntity(MIDDLEWARE_URL+"/validartoken", validationRequest, Map.class);

            if (Boolean.TRUE.equals(validationResponse.getBody().get("valido"))) {
                String usuario = (String) validationResponse.getBody().get("idusuario");
                List<String> roles = (List<String>) validationResponse.getBody().get("roles");
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // 3. continuar la cadena de filtros
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return List.of(
                "/logInEnd",
                "/logOff",
                "/notices",
                "/contact"
        ).contains(path);
    }

}