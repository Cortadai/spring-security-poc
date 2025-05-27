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
public class ClaimsController {

    private final RestTemplate restTemplate;

    @GetMapping("/getClaims")
    public ResponseEntity<String> getClaims(HttpServletRequest request) {
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            throw new MiddlewareException("Falta la cabecera X-Idsession");
        }

        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String accesoCookie = SessionUtil.getCookieValue(request, ACCESS_COOKIE + idsession);

        if (sessionCookie == null || accesoCookie == null) {
            throw new MiddlewareException("Faltan cookies de sesión o acceso");
        }

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
            return ResponseEntity.status(HttpStatus.OK).body(responseMW.getBody());

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware al obtener claims: {}", e.getStatusCode());
            throw new MiddlewareException("Error al recuperar claims del middleware");
        } catch (Exception e) {
            log.error("Error inesperado obteniendo claims", e);
            throw new RuntimeException("Error interno al recuperar claims");
        }
    }
}
