package com.entelgy.bank.controller.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.entelgy.bank.util.SessionUtil;

import static com.entelgy.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClaimsController {

    private final RestTemplate restTemplate;

    @GetMapping("/getClaims")
    public ResponseEntity<String> getClaims(HttpServletRequest request, HttpServletResponse response) {
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Idsession");
        }

        // Buscar cookies relevantes
        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String accesoCookie = SessionUtil.getCookieValue(request, ACCESS_COOKIE + idsession);

        if (sessionCookie == null || accesoCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Faltan cookies de sesión/acceso");
        }

        // Preparar headers para reenviar al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                ACCESS_COOKIE + idsession + "=" + accesoCookie);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> responseMW = restTemplate.exchange(
                    MIDDLEWARE_URL + "/obtenerclaims1",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return ResponseEntity.status(responseMW.getStatusCode()).body(responseMW.getBody());
        } catch (Exception e) {
            log.error("Error al contactar con el middleware", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
