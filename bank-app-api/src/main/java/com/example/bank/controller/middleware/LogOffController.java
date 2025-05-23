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
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String protectionCookie = SessionUtil.getCookieValue(request, PROTECTION_COOKIE + idsession);
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (sessionCookie == null || protectionCookie == null || token == null || token.isBlank()) {
            log.warn("Faltan cookies o token: sesion={}, IP={}", idsession, clientIp);
            throw new MiddlewareException("Faltan cookies de sesión/protección o token de autorización");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                PROTECTION_COOKIE + idsession + "=" + protectionCookie);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);
        headers.set(HttpHeaders.AUTHORIZATION, token); // ✅ Token encriptado

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/logoff2";

        try {
            log.debug("Enviando logout al middleware: sesion={}, url={}", idsession, url);
            ResponseEntity<String> mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Copiar cookies de borrado
            List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String cookie : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie);
                }
            }

            // Vaciar headers de sesión
            response.setHeader(XIDSESSION, "");
            response.setHeader(HttpHeaders.AUTHORIZATION, "");

            log.info("Logout completado exitosamente: sesion={}, IP={}", idsession, clientIp);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware durante logoff: sesion={}, IP={}, status={}", idsession, clientIp, e.getStatusCode());
            throw new MiddlewareException("Error de autenticación al hacer logout");
        } catch (Exception e) {
            log.error("Error inesperado al contactar middleware: sesion={}, IP={}, error={}", idsession, clientIp, e.getMessage());
            throw new RuntimeException("Error interno durante el logout");
        }
    }


}
