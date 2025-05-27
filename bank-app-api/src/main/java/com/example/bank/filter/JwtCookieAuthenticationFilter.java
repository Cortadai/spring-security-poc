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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.bank.constants.ApplicationConstants.ACCESS_COOKIE;
import static com.example.bank.constants.ApplicationConstants.MIDDLEWARE_URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String clientIp = request.getRemoteAddr();
        String idsession = request.getHeader("X-Idsession");

        log.debug("Validando token para: ruta={}, sesion={}, IP={}", path, idsession, clientIp);

        String token = null;

        // Buscar cookie de acceso
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().startsWith(ACCESS_COOKIE)) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(cookie.getValue());
                        token = new String(decoded);
                        log.debug("Cookie de acceso encontrada: sesion={}, cookie={}", 
                                 idsession, cookie.getName());
                        break;
                    } catch (Exception e) {
                        log.warn("Error decodificando cookie JWT de acceso: sesion={}, IP={}, error={}", 
                                idsession, clientIp, e.getMessage());
                        throw new MiddlewareException("Token mal formado");
                    }
                }
            }
        }

        // Si hay token, validarlo con el middleware
        if (token != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, String> body = Map.of("token", token);
                HttpEntity<Map<String, String>> validationRequest = new HttpEntity<>(body, headers);

                log.debug("Enviando token para validación: sesion={}, tokenSufijo={}", 
                         idsession, token.substring(Math.max(0, token.length() - 10)));

                ResponseEntity<Map> validationResponse = restTemplate
                        .postForEntity(MIDDLEWARE_URL + "/validartoken", validationRequest, Map.class);

                Map<String, Object> responseBody = validationResponse.getBody();

                if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("valido"))) {
                    log.warn("Token inválido según middleware: sesion={}, IP={}", idsession, clientIp);
                    throw new MiddlewareException("Token inválido según el middleware");
                }

                // Si es válido, establecer autenticación
                String usuario = (String) responseBody.get("idusuario");
                List<String> roles = (List<String>) responseBody.get("roles");
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());

                log.info("Token validado correctamente: usuario={}, sesion={}, ruta={}, IP={}", 
                         usuario, idsession, path, clientIp);
                log.debug("Roles asignados: usuario={}, roles={}", usuario, roles);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (HttpClientErrorException ex) {
                log.warn("Error del middleware al validar token: sesion={}, IP={}, statusCode={}", 
                        idsession, clientIp, ex.getStatusCode());
                throw new MiddlewareException("Token rechazado por el middleware");
            } catch (Exception ex) {
                log.error("Error inesperado validando el token: sesion={}, IP={}, error={}", 
                         idsession, clientIp, ex.getMessage());
                throw new MiddlewareException("Error interno al validar el token");
            }
        } else {
            log.debug("No se encontró cookie de acceso: ruta={}, IP={}", path, clientIp);
        }

        // Continuar si fue bien
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
