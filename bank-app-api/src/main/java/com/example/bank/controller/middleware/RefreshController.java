package com.example.bank.controller.middleware;

import com.example.bank.exception.MiddlewareException;
import com.example.bank.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static com.example.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RefreshController {

    private final RestTemplate restTemplate;

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = request.getRemoteAddr();
        String idsession = SessionUtil.getSessionId(request);
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.info("Iniciando proceso de refresco de token: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        if (token == null || token.isBlank()) {
            throw new MiddlewareException("Falta la cabecera Authorization");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String protectionCookie = SessionUtil.getCookieValue(request, PROTECTION_COOKIE + idsession);

        if (sessionCookie == null || protectionCookie == null) {
            throw new MiddlewareException("Faltan cookies de sesión o protección");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE,
                SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                        PROTECTION_COOKIE + idsession + "=" + protectionCookie);
        headers.set(XCERTAUTH, FAKECERT);
        headers.set(XIDSESSION, idsession);
        headers.set(HttpHeaders.AUTHORIZATION, token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/refresco2";

        try {
            log.debug("Enviando solicitud de refresco al middleware: sesion={}, url={}", idsession, url);

            ResponseEntity<String> mwResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // El middleware devuelve el nuevo token encriptado en Authorization
            String nuevoToken = mwResponse.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (nuevoToken != null) {
                response.setHeader(HttpHeaders.AUTHORIZATION, nuevoToken);
            }

            log.info("Token refrescado exitosamente: sesion={}, IP={}", idsession, clientIp);

            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware durante el refresco: sesion={}, IP={}, status={}",
                    idsession, clientIp, e.getStatusCode());
            throw new MiddlewareException("Error de autenticación al refrescar el token");
        } catch (Exception e) {
            log.error("Error inesperado al contactar con el middleware para refresco: sesion={}, IP={}, error={}",
                    idsession, clientIp, e.getMessage());
            throw new RuntimeException("Error interno durante el refresco del token");
        }
    }

    @GetMapping("/expires")
    public ResponseEntity<Boolean> expires(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String idsession = SessionUtil.getSessionId(request);
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.info("Verificando expiración de token: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        if (token == null || token.isBlank()) {
            throw new MiddlewareException("Falta la cabecera Authorization");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(XCERTAUTH, FAKECERT);
        headers.set(XIDSESSION, idsession);
        headers.set(HttpHeaders.AUTHORIZATION, token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/expira2";

        try {
            log.debug("Enviando solicitud de verificación de expiración: sesion={}, url={}", idsession, url);

            ResponseEntity<Boolean> responseMW = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            Boolean expira = responseMW.getBody();
            log.info("Verificación completada: sesion={}, expira={}, IP={}", idsession, expira, clientIp);

            return ResponseEntity.status(responseMW.getStatusCode()).body(expira);

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware al comprobar expiración: sesion={}, IP={}, status={}",
                    idsession, clientIp, e.getStatusCode());
            throw new MiddlewareException("Error al comprobar expiración del token");
        } catch (Exception e) {
            log.error("Error inesperado al verificar expiración: sesion={}, IP={}, error={}",
                    idsession, clientIp, e.getMessage());
            throw new RuntimeException("Error interno durante la comprobación de expiración");
        }
    }

}
