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
public class SessionController {

    private final RestTemplate restTemplate;

    @GetMapping("/getSessionState")
    public ResponseEntity<Void> getSessionState(HttpServletRequest request, HttpServletResponse response) {
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Buscar cookie usando SessionUtil
        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        if (sessionCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Preparar llamada al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie);
        headers.set(XIDSESSION, idsession);
        headers.set(XCERTAUTH, FAKECERT);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> middlewareResponse = restTemplate.exchange(
                    MIDDLEWARE_URL + "/estadosession",
                    HttpMethod.GET,
                    entity,
                    Void.class
            );
            return ResponseEntity.status(middlewareResponse.getStatusCode()).build();
        } catch (Exception e) {
            log.warn("Error al contactar con el middleware", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
