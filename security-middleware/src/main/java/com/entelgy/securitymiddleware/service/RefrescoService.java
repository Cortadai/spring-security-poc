package com.entelgy.securitymiddleware.service;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefrescoService {

    private final TokenProvider tokenProvider;
    private final SecurityInfoService securityInfoService;

    public void procesarRefresco(HttpServletRequest request, HttpServletResponse response, String idsession, String certAuth) {
        String sessionCookieName = "Session-" + idsession;
        String accessCookieName = "Acceso-" + idsession;

        String sessionCookieValue = null;
        String accessCookieValue = null;

        // Buscar cookies en la request
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionCookieValue = cookie.getValue();
                }
                if (accessCookieName.equals(cookie.getName())) {
                    accessCookieValue = cookie.getValue();
                }
            }
        }

        if (sessionCookieValue == null || accessCookieValue == null) {
            throw new SecurityException("Faltan cookies de sesión o acceso");
        }

        try {
            // Decode base64 de la cookie de sesión: contiene token sesión y refresco
            String decodedSession = new String(Base64.getDecoder().decode(sessionCookieValue), StandardCharsets.UTF_8);
            String[] sessionParts = decodedSession.split("::");
            if (sessionParts.length < 2) {
                throw new SecurityException("La cookie de sesión no contiene los dos tokens esperados");
            }
            String jwtSession = sessionParts[0];
            String jwtRefresh = sessionParts[1];

            // Decode base64 de la cookie de acceso
            String jwtAccess = new String(Base64.getDecoder().decode(accessCookieValue), StandardCharsets.UTF_8);

            // Validar tokens
            if (!tokenProvider.validateToken(jwtSession)) throw new SecurityException("Token de sesión inválido");
            if (!tokenProvider.validateToken(jwtRefresh)) throw new SecurityException("Token de refresco inválido");
            if (!tokenProvider.validateToken(jwtAccess)) throw new SecurityException("Token de acceso inválido");

            // Extraer claims
            Claims sessionClaims = tokenProvider.parseToken(jwtSession);
            Claims refreshClaims = tokenProvider.parseToken(jwtRefresh);
            Claims accessClaims = tokenProvider.parseToken(jwtAccess);

            int actualRefresco = accessClaims.get("NumeroRefresco", Integer.class);
            int maxRefrescos = refreshClaims.get("MaxRefrescos", Integer.class);
            if (actualRefresco >= maxRefrescos) {
                throw new SecurityException("Se alcanzó el número máximo de refrescos permitidos");
            }

            String idUsuario = sessionClaims.get("idusuario", String.class);
            String idAplicacion = sessionClaims.get("idaplicacion", String.class);

            // Obtener roles actualizados
            List<String> roles = securityInfoService.getRolesForUser(idUsuario, idAplicacion);

            // Generar nuevo token de acceso con NumeroRefresco++
            int siguienteRefresco = actualRefresco + 1;
            String nuevoJwtAccess = tokenProvider.generateAccessToken(idUsuario, idAplicacion, idsession, roles, siguienteRefresco);

            // Codificar en Base64
            String nuevoAccessValue = Base64.getEncoder().encodeToString(nuevoJwtAccess.getBytes(StandardCharsets.UTF_8));

            // Crear nueva cookie
            ResponseCookie nuevaAccessCookie = ResponseCookie.from("Acceso-" + idsession, nuevoAccessValue)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .build();

            // Añadir al response
            response.addHeader("Set-Cookie", nuevaAccessCookie.toString());

        } catch (Exception e) {
            log.error("Error al procesar refresco: {}", e.getMessage());
            throw new SecurityException("Error durante el refresco del token de acceso");
        }
    }
}
