package com.entelgy.bank.controller.middleware;

import com.entelgy.bank.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.entelgy.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LogOffController {

    private final RestTemplate restTemplate;

    @GetMapping("/logOff")
    public ResponseEntity<Void> logOff(HttpServletRequest request, HttpServletResponse response) {
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

        // Construir cabeceras
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, SESSION_COOKIE + idsession + "=" + sessionCookie + "; " +
                ACCESS_COOKIE + idsession + "=" + accesoCookie);
        headers.add(XCERTAUTH, FAKECERT);
        headers.add(XIDSESSION, idsession);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = MIDDLEWARE_URL+"/logoff1";
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
                response.addHeader(SET_COOKIE, cookie);
            }
        }

        // Copiar cabecera X-Idsession vacía
        String idsessionHeader = mwResponse.getHeaders().getFirst(XIDSESSION);

        return ResponseEntity.status(HttpStatus.OK)
                .header(XIDSESSION, idsessionHeader != null ? idsessionHeader : "")
                .build();
    }
}
