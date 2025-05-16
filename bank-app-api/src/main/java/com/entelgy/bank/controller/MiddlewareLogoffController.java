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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MiddlewareLogoffController {

    private final RestTemplate restTemplate;

    @GetMapping("/fin-logoff")
    public ResponseEntity<Void> finLogoff(HttpServletRequest request, HttpServletResponse response) {
        String idsession = request.getHeader("X-Idsession");
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Extraer cookies relevantes
        String sessionCookieName = "Session-" + idsession;
        String accesoCookieName = "Acceso-" + idsession;
        String sessionCookie = null;
        String accesoCookie = null;

        for (Cookie cookie : request.getCookies()) {
            if (sessionCookieName.equals(cookie.getName())) {
                sessionCookie = cookie.getValue();
            }
            if (accesoCookieName.equals(cookie.getName())) {
                accesoCookie = cookie.getValue();
            }
        }

        if (sessionCookie == null || accesoCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Construir cabeceras
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookieName + "=" + sessionCookie + "; " + accesoCookieName + "=" + accesoCookie);
        headers.add("X-Cert-Auth", "FAKE-CERT-FOR-POC"); // sustituir si se implementa validación real
        headers.add("X-Idsession", idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:7070/logoff1";
        ResponseEntity<String> mwResponse;
        try {
            mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            log.error("Error al contactar con el middleware", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Copiar cookies para borrado
        List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            for (String cookie : setCookies) {
                response.addHeader("Set-Cookie", cookie);
            }
        }

        // Copiar cabecera X-Idsession vacía
        String idsessionHeader = mwResponse.getHeaders().getFirst("X-Idsession");

        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Idsession", idsessionHeader != null ? idsessionHeader : "")
                .build();
    }
}
