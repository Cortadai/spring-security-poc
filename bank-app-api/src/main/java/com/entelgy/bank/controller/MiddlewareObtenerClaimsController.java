package com.entelgy.bank.controller;

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
public class MiddlewareObtenerClaimsController {

    private final RestTemplate restTemplate;

    @GetMapping("/obtenerclaimsSPA")
    public ResponseEntity<String> obtenerClaimsSPA(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Idsession");
        }

        // Buscar cookies relevantes
        String sessionCookieName = "Session-" + idsession;
        String accesoCookieName = "Acceso-" + idsession;
        String sessionCookie = null;
        String accesoCookie = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionCookie = cookie.getValue();
                }
                if (accesoCookieName.equals(cookie.getName())) {
                    accesoCookie = cookie.getValue();
                }
            }
        }

        if (sessionCookie == null || accesoCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Faltan cookies de sesión/acceso");
        }

        // Preparar headers para reenviar al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookieName + "=" + sessionCookie + "; " + accesoCookieName + "=" + accesoCookie);
        headers.add("X-Cert-Auth", "FAKE-CERT-FOR-POC");
        headers.add("X-Idsession", idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> responseMW = restTemplate.exchange(
                    "http://localhost:7070/obtenerclaims1",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return ResponseEntity.status(responseMW.getStatusCode())
                    .body(responseMW.getBody());

        } catch (Exception e) {
            log.error("Error llamando al middleware en /obtenerclaims1", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al contactar con el middleware");
        }
    }
}
