package com.example.bank.controller.middleware;

import com.example.bank.exception.MiddlewareException;
import com.example.bank.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static com.example.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final RestTemplate restTemplate;

    @GetMapping("/getSessionState")
    public ResponseEntity<?> getSessionState(HttpServletRequest request) {
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        if (sessionCookie == null) {
            throw new MiddlewareException("Falta la cookie " + SESSION_COOKIE + idsession);
        }

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

        } catch (HttpClientErrorException e) {
            log.warn("Error 4xx del middleware: {}", e.getStatusCode());
            throw new MiddlewareException("Error al validar la sesión con el middleware");
        } catch (Exception e) {
            log.error("Error inesperado validando la sesión", e);
            throw new RuntimeException("Error interno durante la validación de sesión");
        }
    }
}
