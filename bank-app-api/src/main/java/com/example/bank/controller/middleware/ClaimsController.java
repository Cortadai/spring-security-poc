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
        String protectionCookie = SessionUtil.getCookieValue(request, PROTECTION_COOKIE + idsession);
        String encryptedToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (sessionCookie == null || protectionCookie == null || encryptedToken == null || encryptedToken.isBlank()) {
            throw new MiddlewareException("Faltan cookies o cabeceras requeridas");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                PROTECTION_COOKIE + idsession + "=" + protectionCookie);
        headers.set(HttpHeaders.AUTHORIZATION, encryptedToken);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> responseMW = restTemplate.exchange(
                    MIDDLEWARE_URL + "/obtenerclaims2",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return ResponseEntity.ok(responseMW.getBody());

        } catch (HttpClientErrorException e) {
            log.warn("Error del middleware al obtener claims: {}", e.getStatusCode());
            throw new MiddlewareException("Error al recuperar claims del middleware");
        } catch (Exception e) {
            log.error("Error inesperado obteniendo claims", e);
            throw new RuntimeException("Error interno al recuperar claims");
        }
    }

}
