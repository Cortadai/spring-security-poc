package com.entelgy.bank.controller.middleware;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final RestTemplate restTemplate;

    @GetMapping("/getSessionState")
    public ResponseEntity<Void> getSessionState(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String sessionCookieName = "Session-" + idsession;
        String sessionCookie = null;

        // Buscar cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionCookie = cookie.getValue();
                    break;
                }
            }
        }

        if (sessionCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Preparar llamada al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookieName + "=" + sessionCookie);
        headers.set("X-Idsession", idsession);
        headers.set("X-Cert-Auth", "FAKE-CERT-FOR-POC");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> middlewareResponse = restTemplate.exchange(
                    "http://localhost:7070/estadosession",
                    HttpMethod.GET,
                    entity,
                    Void.class
            );

            return ResponseEntity.status(middlewareResponse.getStatusCode()).build();

        } catch (Exception e) {
            log.warn("Error al contactar con el middleware en /estadosession: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
