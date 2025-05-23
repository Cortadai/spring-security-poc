package com.example.bank.filter;

import com.example.bank.exception.MiddlewareException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.bank.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAndTokenAuthenticationFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String clientIp = request.getRemoteAddr();
        String idsession = request.getHeader(XIDSESSION);
        String encryptedToken = request.getHeader(AUTHORIZATION);
        String fingerprint = null;

        // Leer fingerprint desde cookie Proteccion-{idsession}
        if (request.getCookies() != null && idsession != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(PROTECTION_COOKIE + idsession)) {
                    fingerprint = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (idsession == null || encryptedToken == null || fingerprint == null) {
            log.debug("Faltan elementos clave para autenticación (opción 2): ruta={}, IP={}", path, clientIp);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Preparar llamada al middleware
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("token", encryptedToken);
            body.put("idsession", idsession);
            body.put("fingerprint", fingerprint);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> responseEntity = restTemplate
                    .postForEntity(MIDDLEWARE_URL + "/validartoken", requestEntity, Map.class);

            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("valido"))) {
                log.warn("Token inválido según middleware (opción 2): IP={}, sesion={}", clientIp, idsession);
                throw new MiddlewareException("Token inválido");
            }

            // Crear autenticación
            String usuario = (String) responseBody.get("idusuario");
            List<String> roles = (List<String>) responseBody.get("roles");

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(usuario, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("Autenticación completada: usuario={}, sesion={}, IP={}", usuario, idsession, clientIp);

        } catch (HttpClientErrorException ex) {
            log.warn("Middleware rechazó token: sesion={}, IP={}, status={}", idsession, clientIp, ex.getStatusCode());
            throw new MiddlewareException("Token rechazado por el middleware");
        } catch (Exception ex) {
            log.error("Error validando token opción 2: sesion={}, IP={}, error={}", idsession, clientIp, ex.getMessage());
            throw new MiddlewareException("Error interno en autenticación");
        }

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
