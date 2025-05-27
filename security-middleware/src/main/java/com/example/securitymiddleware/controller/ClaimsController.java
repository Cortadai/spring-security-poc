package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;

@Slf4j
@RestController
public class ClaimsController extends BaseController {

    public ClaimsController(TokenProvider tokenProvider) {
        super(tokenProvider);
    }

    @GetMapping("/obtenerclaims1")
    public Map<String, Object> obtenerClaims(HttpServletRequest request) {
        String idsession = request.getHeader(XIDSESSION);
        if (idsession == null || idsession.isBlank()) {
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }

        String sessionCookieValue = getCookieValue(request, SESSION_COOKIE + idsession);
        String accessCookieValue = getCookieValue(request, ACCESS_COOKIE + idsession);
        if (sessionCookieValue == null || accessCookieValue == null) {
            throw new CookieNotFoundException("Faltan cookies de sesión o acceso");
        }

        String decodedSession = decodeBase64(sessionCookieValue);
        String[] sessionParts = decodedSession.split("::");
        if (sessionParts.length < 1) throw new RuntimeException("La cookie de sesión está mal formada");

        String jwtSession = sessionParts[0];
        String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;
        String jwtAccess = decodeBase64(accessCookieValue);

        Claims sessionClaims = parseValidToken(jwtSession, "Token de sesión inválido");
        Claims refreshClaims = jwtRefresh != null ? parseValidToken(jwtRefresh, "Token de refresco inválido") : null;
        Claims accessClaims = parseValidToken(jwtAccess, "Token de acceso inválido");

        Map<String, Object> result = new HashMap<>();
        result.put("session", sessionClaims);
        result.put("refresh", refreshClaims);
        result.put("access", accessClaims);
        return result;
    }

}
