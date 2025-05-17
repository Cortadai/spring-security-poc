package com.entelgy.bank.controller.middleware;

import com.entelgy.bank.util.SessionUtil;
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

import static com.entelgy.bank.constants.ApplicationConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LogInController {

    private final RestTemplate restTemplate;

    @GetMapping("/logInEnd")
    public ResponseEntity<Void> loginEnd(HttpServletRequest request, HttpServletResponse response) {
        // Leer Sessiontmp usando SessionUtil
        String sessiontmp = SessionUtil.getCookieValue(request, SESSIONTMP);
        if (sessiontmp == null || sessiontmp.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Preparar llamada al middleware
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, SESSIONTMP+"=" + sessiontmp);
        headers.add(XCERTAUTH, FAKECERT);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = MIDDLEWARE_URL+"/login1End";
        ResponseEntity<String> mwResponse;
        try {
            mwResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            log.error("Error al contactar con el middleware", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Copiar las cookies del middleware
        List<String> setCookies = mwResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            for (String cookie : setCookies) {
                response.addHeader(SET_COOKIE, cookie);
            }
        }

        // Copiar cabecera X-Idsession
        String idsession = mwResponse.getHeaders().getFirst(XIDSESSION);
        response.addHeader(XIDSESSION, idsession != null ? idsession : "");
        
        // 5. Borrar Sessiontmp (opcional, ya lo hace el middleware)
        Cookie borrar = new Cookie(SESSIONTMP, "");
        borrar.setMaxAge(0);
        borrar.setPath("/");
        response.addCookie(borrar);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
