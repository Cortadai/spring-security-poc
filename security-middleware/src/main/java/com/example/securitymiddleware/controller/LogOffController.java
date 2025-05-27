package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.repository.TokenRepository;
import com.example.securitymiddleware.service.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@Slf4j
@RestController
public class LogOffController extends BaseController {

    private final TokenRepository tokenRepository;

    public LogOffController(TokenProvider tokenProvider, TokenRepository tokenRepository) {
        super(tokenProvider);
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/logoff1")
    public ResponseEntity<?> logoff1(HttpServletRequest request) {
        String certHeader = request.getHeader(XCERTAUTH);
        String idsession = request.getHeader(XIDSESSION);
        String clientIp = request.getRemoteAddr();

        log.info("Iniciando proceso de logout: sesion={}, IP={}", idsession, clientIp);

        if (certHeader == null || certHeader.isBlank()) {
            log.warn("Intento de logout sin certificado: sesion={}, IP={}", idsession, clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }
        if (idsession == null || idsession.isBlank()) {
            log.warn("Intento de logout sin idsession: IP={}", clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }

        String sessionCookieName = SESSION_COOKIE + idsession;
        String accessCookieName = ACCESS_COOKIE + idsession;

        String sessionCookieValue = getCookieValue(request, sessionCookieName);
        String accessCookieValue = getCookieValue(request, accessCookieName);

        if (sessionCookieValue == null || accessCookieValue == null) {
            log.warn("Intento de logout sin cookies requeridas: sesion={}, IP={}", idsession, clientIp);
            throw new CookieNotFoundException("Faltan cookies de sesión o acceso para idsession=" + idsession);
        }

        try {
            log.debug("Decodificando tokens para logout: sesion={}, IP={}", idsession, clientIp);

            String decodedSession = decodeBase64(sessionCookieValue);
            String[] sessionParts = decodedSession.split("::");
            String jwtSesion = sessionParts[0];
            String jwtRefresh = sessionParts.length > 1 ? sessionParts[1] : null;
            String jwtAccess = decodeBase64(accessCookieValue);

            if (!tokenProvider.validateToken(jwtSesion)) {
                log.warn("Token de sesión caducado, pero continuamos con el logout: sesion={}, IP={}", 
                         idsession, clientIp);
            }

            String jtiAccess = tokenProvider.getJtiFromToken(jwtAccess);
            String jtiRefresh = jwtRefresh != null ? tokenProvider.getJtiFromToken(jwtRefresh) : null;

            log.debug("Revocando tokens: sesion={}, jtiAccess={}, jtiRefresh={}", 
                     idsession, jtiAccess, jtiRefresh);

            tokenRepository.removeTokens(idsession, jtiAccess, jtiRefresh);

            log.info("Tokens revocados correctamente: sesion={}, jtiAccess={}, jtiRefresh={}, IP={}", 
                     idsession, jtiAccess, jtiRefresh, clientIp);

        } catch (Exception e) {
            log.warn("Error durante logoff: sesion={}, IP={}, error={}", 
                     idsession, clientIp, e.getMessage());
            throw new RuntimeException("Error durante logoff: " + e.getMessage(), e);
        }

        log.debug("Creando cookies de eliminación para: sesion={}", idsession);

        ResponseCookie deleteSession = ResponseCookie.from(sessionCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteAccess = ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        log.info("Logout completado exitosamente: sesion={}, IP={}", idsession, clientIp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(SET_COOKIE, deleteSession.toString())
                .header(SET_COOKIE, deleteAccess.toString())
                .header(XIDSESSION, "")
                .build();
    }
}
