package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.repository.TokenRepository;
import com.example.securitymiddleware.service.SecurityInfoService;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@Slf4j
@RestController
public class LogInEndController extends BaseController {

    @Value("${jwt.maxRefresh}")
    private int maxRefresh;

    private final SecurityInfoService securityInfoService;
    private final TokenRepository tokenRepository;

    public LogInEndController(TokenProvider tokenProvider, SecurityInfoService securityInfoService, TokenRepository tokenRepository) {
        super(tokenProvider);
        this.securityInfoService = securityInfoService;
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/login1End")
    public ResponseEntity<?> login1End(HttpServletRequest request) {
        String sessiontmp = getCookieValue(request, SESSIONTMP);
        String certHeader = request.getHeader(XCERTAUTH);
        String clientIp = request.getRemoteAddr();

        log.info("Completando login: IP={}", clientIp);

        if (sessiontmp == null || sessiontmp.isBlank()) {
            log.warn("Intento de completar login sin cookie Sessiontmp: IP={}", clientIp);
            throw new CookieNotFoundException("Falta la cookie Sessiontmp");
        }
        if (certHeader == null || certHeader.isBlank()) {
            log.warn("Intento de completar login sin certificado: IP={}", clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }

        Claims claims;
        try {
            claims = tokenProvider.parseToken(sessiontmp);
            log.debug("Token temporal decodificado correctamente: IP={}", clientIp);
        } catch (Exception e) {
            log.warn("Error al decodificar token temporal: IP={}, error={}", clientIp, e.getMessage());
            throw new TokenValidationException("Token Sessiontmp inválido: " + e.getMessage());
        }

        if (!tokenProvider.validateToken(sessiontmp)) {
            log.warn("Token temporal inválido o expirado: IP={}", clientIp);
            throw new TokenValidationException("Token de sesión temporal inválido");
        }

        String idusuario = claims.getSubject();
        String idaplicacion = claims.get("idaplicacion", String.class);
        String idsession = claims.get("idsession", String.class);

        log.info("Validación de token temporal exitosa: usuario={}, aplicacion={}, sesion={}, IP={}", 
                 idusuario, idaplicacion, idsession, clientIp);

        log.debug("Generando tokens para usuario: usuario={}, aplicacion={}, sesion={}, IP={}", 
                 idusuario, idaplicacion, idsession, clientIp);

        String jwtSesion = tokenProvider.generateSessionToken(idusuario, idaplicacion, idsession);
        String jwtRefresh = tokenProvider.generateRefreshToken(idusuario, idaplicacion, idsession, maxRefresh);
        List<String> roles = securityInfoService.getRolesForUser(idusuario, idaplicacion);
        String jwtAcceso = tokenProvider.generateAccessToken(idusuario, idaplicacion, idsession, roles, 0);

        log.debug("Roles asignados: usuario={}, roles={}, sesion={}", idusuario, roles, idsession);

        tokenRepository.storeAccessToken(idsession, jwtAcceso);
        tokenRepository.storeRefreshToken(idsession, jwtRefresh);

        log.info("Tokens generados correctamente: sesion={}, usuario={}, IP={}", 
                 idsession, idusuario, clientIp);

        String sessionCookieValue = Base64.getEncoder().encodeToString((jwtSesion + "::" + jwtRefresh).getBytes(StandardCharsets.UTF_8));
        String accessCookieValue = Base64.getEncoder().encodeToString(jwtAcceso.getBytes(StandardCharsets.UTF_8));

        ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE + idsession, sessionCookieValue)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .build();

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE + idsession, accessCookieValue)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .build();

        ResponseCookie deleteTmp = ResponseCookie.from(SESSIONTMP, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();

        log.info("Login completado exitosamente: usuario={}, aplicacion={}, sesion={}, IP={}", 
                 idusuario, idaplicacion, idsession, clientIp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(SET_COOKIE, sessionCookie.toString())
                .header(SET_COOKIE, accessCookie.toString())
                .header(SET_COOKIE, deleteTmp.toString())
                .header(XIDSESSION, idsession)
                .build();
    }
}
