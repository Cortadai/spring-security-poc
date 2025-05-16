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
public class MiddlewareLogInController {

    private final RestTemplate restTemplate;

    @GetMapping("/fin-login")
    public ResponseEntity<Void> finLogin(HttpServletRequest request, HttpServletResponse response) {
        // 1. Leer Sessiontmp
        String sessiontmp = null;
        for (Cookie cookie : request.getCookies()) {
            if ("Sessiontmp".equals(cookie.getName())) {
                sessiontmp = cookie.getValue();
                break;
            }
        }
        if (sessiontmp == null || sessiontmp.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Preparar llamada al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "Sessiontmp=" + sessiontmp);
        headers.add("X-Cert-Auth", "FAKE-CERT-FOR-POC");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:7070/login1End";
        ResponseEntity<String> mwResponse;
        try {
            mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            log.error("Error al contactar con el middleware", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 3. Copiar las cookies del middleware
        List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            for (String cookie : setCookies) {
                response.addHeader("Set-Cookie", cookie);
            }
        }

        // 4. Copiar cabecera X-Idsession
        String idsession = mwResponse.getHeaders().getFirst("X-Idsession");

        // 5. Borrar Sessiontmp (opcional, ya lo hace el middleware)
        Cookie borrar = new Cookie("Sessiontmp", "");
        borrar.setMaxAge(0);
        borrar.setPath("/");
        response.addCookie(borrar);

        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Idsession", idsession != null ? idsession : "")
                .build();
    }

}
