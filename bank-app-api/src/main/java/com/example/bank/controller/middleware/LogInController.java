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
public class LogInController {

    private final RestTemplate restTemplate;

    @GetMapping("/logInEnd")
    public ResponseEntity<?> loginEnd(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = request.getRemoteAddr();
        String sessiontmp = SessionUtil.getCookieValue(request, SESSIONTMP);

        log.info("Procesando login desde backend: IP={}", clientIp);

        if (sessiontmp == null || sessiontmp.isBlank()) {
            log.warn("Intento de login sin cookie temporal: IP={}", clientIp);
            throw new MiddlewareException("Falta la cookie " + SESSIONTMP);
        }

        log.debug("Cookie temporal encontrada, contactando middleware: IP={}", clientIp);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, SESSIONTMP + "=" + sessiontmp);
        headers.add(XCERTAUTH, FAKECERT);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = MIDDLEWARE_URL + "/login2End";

        ResponseEntity<String> mwResponse;
        try {
            log.debug("Enviando solicitud al middleware: url={}, IP={}", url, clientIp);
            mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.debug("Respuesta recibida del middleware: status={}", mwResponse.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.warn("Error 4xx del middleware: status={}, IP={}", e.getStatusCode(), clientIp);
            throw new MiddlewareException("Token rechazado por el middleware");
        } catch (Exception e) {
            log.error("Error inesperado al contactar con el middleware: IP={}, error={}", 
                     clientIp, e.getMessage());
            throw new RuntimeException("No se pudo contactar con el middleware");
        }

        // Copiar cookies
        List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            log.debug("Copiando {} cookies del middleware", setCookies.size());
            for (String cookie : setCookies) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookie);
            }
        }

        // Copiar cabecera Authorization
        String jwtAcceso = mwResponse.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (jwtAcceso != null) {
            log.debug("Copiando header Authorization (token encriptado)");
            response.setHeader(HttpHeaders.AUTHORIZATION, jwtAcceso);
        }

        // Copiar header X-Idsession
        String idsession = mwResponse.getHeaders().getFirst(XIDSESSION);
        if (idsession != null) {
            log.debug("Copiando header X-Idsession: {}", idsession);
            response.setHeader(XIDSESSION, idsession);
        }

        log.info("Login completado exitosamente: sesion={}, IP={}", idsession, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
