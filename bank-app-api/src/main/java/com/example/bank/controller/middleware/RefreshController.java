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

import java.util.List;

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

        log.info("Iniciando proceso de refresco de token: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            log.warn("Intento de refresco sin idsession: IP={}", clientIp);
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String accesoCookie = SessionUtil.getCookieValue(request, ACCESS_COOKIE + idsession);

        if (sessionCookie == null || accesoCookie == null) {
            log.warn("Intento de refresco sin cookies requeridas: sesion={}, IP={}", idsession, clientIp);
            throw new MiddlewareException("Faltan cookies de sesión o acceso");
        }

        log.debug("Cookies de sesión encontradas, contactando middleware: sesion={}", idsession);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                ACCESS_COOKIE + idsession + "=" + accesoCookie);
        headers.set(XCERTAUTH, FAKECERT);
        headers.set(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/refresco1";

        try {
            log.debug("Enviando solicitud de refresco al middleware: sesion={}, url={}", idsession, url);
            ResponseEntity<String> mwResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            log.debug("Respuesta de refresco recibida del middleware: status={}", mwResponse.getStatusCode());

            List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                log.debug("Copiando {} cookies del middleware", setCookies.size());
                for (String cookie : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie);
                }
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

        log.info("Verificando expiración de token: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            log.warn("Intento de verificar expiración sin idsession: IP={}", clientIp);
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String cookieName = ACCESS_COOKIE + idsession;
        String cookieValue = SessionUtil.getCookieValue(request, cookieName);

        if (cookieValue == null) {
            log.warn("Intento de verificar expiración sin cookie de acceso: sesion={}, IP={}", idsession, clientIp);
            throw new MiddlewareException("Falta la cookie " + cookieName);
        }

        log.debug("Cookie de acceso encontrada, contactando middleware: sesion={}", idsession);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookieName + "=" + cookieValue);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/expira1";

        try {
            log.debug("Enviando solicitud de verificación de expiración: sesion={}, url={}", idsession, url);
            ResponseEntity<Boolean> responseMW = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            Boolean expira = responseMW.getBody();
            log.info("Verificación de expiración completada: sesion={}, expira={}, IP={}", 
                    idsession, expira, clientIp);

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
