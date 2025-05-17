package com.entelgy.bank.controller.middleware;

import jakarta.servlet.http.Cookie;
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

@Slf4j
@RestController
@RequiredArgsConstructor
public class RefreshController {

    private final RestTemplate restTemplate;

    @GetMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookieName + "=" + sessionCookie + "; " + accesoCookieName + "=" + accesoCookie);
        headers.set("X-Cert-Auth", "FAKE-CERT-FOR-POC");
        headers.set("X-Idsession", idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> mwResponse = restTemplate.exchange(
                    "http://localhost:7070/refresco1",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Reenviar nuevas cookies si existen
            List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String cookie : setCookies) {
                    response.addHeader("Set-Cookie", cookie);
                }
            }

            return ResponseEntity.status(mwResponse.getStatusCode()).build();

        } catch (HttpClientErrorException.Forbidden e) {
            // El middleware ha dicho explícitamente: no se puede refrescar más
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error llamando al middleware en /refresco1", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/expires")
    public ResponseEntity<Boolean> expires(HttpServletRequest request) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().body(false);
        }

        String cookieName = "Acceso-" + idsession;
        String cookieValue = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    cookieValue = cookie.getValue();
                    break;
                }
            }
        }

        if (cookieValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        // Preparar cabeceras para reenviar al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookieName + "=" + cookieValue);
        headers.add("X-Cert-Auth", "FAKE-CERT-FOR-POC");
        headers.add("X-Idsession", idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Boolean> responseMW = restTemplate.exchange(
                    "http://localhost:7070/expira1",
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            return ResponseEntity.status(responseMW.getStatusCode()).body(responseMW.getBody());

        } catch (Exception e) {
            log.error("Error llamando al middleware en /expira1", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

}
