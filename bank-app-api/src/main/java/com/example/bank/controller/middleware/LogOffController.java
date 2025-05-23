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
public class LogOffController {

    private final RestTemplate restTemplate;

    @GetMapping("/logOff")
    public ResponseEntity<?> logOff(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = request.getRemoteAddr();
        String idsession = SessionUtil.getSessionId(request);

        log.info("Iniciando proceso de logout: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            log.warn("Intento de logout sin idsession: IP={}", clientIp);
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String accesoCookie = SessionUtil.getCookieValue(request, ACCESS_COOKIE + idsession);

        if (sessionCookie == null || accesoCookie == null) {
            log.warn("Intento de logout sin cookies requeridas: sesion={}, IP={}", idsession, clientIp);
            throw new MiddlewareException("Faltan cookies de sesión o acceso");
        }

        log.debug("Cookies de sesión encontradas, contactando middleware: sesion={}", idsession);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                ACCESS_COOKIE + idsession + "=" + accesoCookie);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/logoff1";

        try {
            log.debug("Enviando solicitud de logout al middleware: sesion={}, url={}", idsession, url);
            ResponseEntity<String> mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.debug("Respuesta de logout recibida del middleware: status={}", mwResponse.getStatusCode());

            // Copiar cookies de borrado
            List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                log.debug("Copiando {} cookies de borrado del middleware", setCookies.size());
                for (String cookie : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie);
                }
            }

            response.setHeader(XIDSESSION, "");

            log.info("Logout completado exitosamente: sesion={}, IP={}", idsession, clientIp);

            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware durante logoff: sesion={}, IP={}, status={}", 
                    idsession, clientIp, e.getStatusCode());
            throw new MiddlewareException("Error de autenticación al hacer logout");
        } catch (Exception e) {
            log.error("Error inesperado al llamar al middleware para logout: sesion={}, IP={}, error={}", 
                    idsession, clientIp, e.getMessage());
            throw new RuntimeException("Error interno durante el logout");
        }
    }

}
