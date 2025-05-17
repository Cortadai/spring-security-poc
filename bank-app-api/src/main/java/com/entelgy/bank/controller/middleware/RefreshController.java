package com.entelgy.bank.controller.middleware;

import com.entelgy.bank.util.SessionUtil;
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

import static com.entelgy.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RefreshController {

    private final RestTemplate restTemplate;

    @GetMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Extraer cookies relevantes usando SessionUtil
        String sessionCookie = SessionUtil.getCookieValue(request, SESSION_COOKIE + idsession);
        String accesoCookie = SessionUtil.getCookieValue(request, ACCESS_COOKIE + idsession);

        if (sessionCookie == null || accesoCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                ACCESS_COOKIE + idsession + "=" + accesoCookie);
        headers.set(XCERTAUTH, FAKECERT);
        headers.set(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> mwResponse = restTemplate.exchange(
                    MIDDLEWARE_URL+"/refresco1",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Reenviar nuevas cookies si existen
            List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String cookie : setCookies) {
                    response.addHeader(SET_COOKIE, cookie);
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
        String idsession = SessionUtil.getSessionId(request);
        if (idsession == null || idsession.isBlank()) {
            return ResponseEntity.badRequest().body(false);
        }

        String cookieName = ACCESS_COOKIE + idsession;
        String cookieValue = SessionUtil.getCookieValue(request, cookieName);

        if (cookieValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        // Preparar cabeceras para reenviar al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookieName + "=" + cookieValue);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Boolean> responseMW = restTemplate.exchange(
                    MIDDLEWARE_URL+"/expira1",
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
