package com.example.securitymiddleware.service;

import com.example.securitymiddleware.exception.CookieNotFoundException;
import com.example.securitymiddleware.exception.TokenParseException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshService {

    private final TokenProvider tokenProvider;
    private final SecurityInfoService securityInfoService;
    private final TokenRepository tokenRepository;

    public void procesarRefresco(HttpServletRequest request, HttpServletResponse response, String idsession, String certAuth) {
        String clientIp = request.getRemoteAddr();
        log.debug("Iniciando procesamiento de refresco: sesion={}, IP={}", idsession, clientIp);

        String sessionCookie = getCookieValue(request, "Session-" + idsession);
        String accessCookie = getCookieValue(request, "Acceso-" + idsession);

        log.debug("Cookies obtenidas para refresco: sesion={}", idsession);

        String[] sessionParts = decodeAndSplit(sessionCookie, "::", 2, "La cookie de sesión no contiene los dos tokens esperados");
        String jwtSession = sessionParts[0];
        String jwtRefresh = sessionParts[1];
        String jwtAccess = decodeBase64(accessCookie);

        log.debug("Tokens decodificados correctamente: sesion={}", idsession);

        Claims sessionClaims = parseValidToken(jwtSession, "Token de sesión inválido");
        Claims refreshClaims = parseValidToken(jwtRefresh, "Token de refresco inválido");
        Claims accessClaims = parseValidToken(jwtAccess, "Token de acceso inválido");

        log.debug("Tokens validados correctamente: sesion={}", idsession);

        int actualRefresco = accessClaims.get("NumeroRefresco", Integer.class);
        int maxRefrescos = refreshClaims.get("MaxRefrescos", Integer.class);

        log.debug("Estado de refresco: actual={}, máximo={}, sesion={}", 
                 actualRefresco, maxRefrescos, idsession);

        if (actualRefresco >= maxRefrescos) {
            log.warn("Máximo de refrescos alcanzado: sesion={}, actual={}, máximo={}, IP={}", 
                    idsession, actualRefresco, maxRefrescos, clientIp);
            throw new TokenValidationException("Se alcanzó el número máximo de refrescos permitidos");
        }

        String idUsuario = sessionClaims.get("idusuario", String.class);
        String idAplicacion = sessionClaims.get("idaplicacion", String.class);

        log.debug("Revocando token de acceso anterior: sesion={}, usuario={}", idsession, idUsuario);
        revocarTokenSiAplica(accessClaims);

        List<String> roles = securityInfoService.getRolesForUser(idUsuario, idAplicacion);
        String nuevoJwtAccess = tokenProvider.generateAccessToken(idUsuario, idAplicacion, idsession, roles, actualRefresco + 1);
        tokenRepository.storeAccessToken(idsession, nuevoJwtAccess);

        String nuevoJti = tokenProvider.getJtiFromToken(nuevoJwtAccess);
        log.info("Nuevo token de acceso emitido: sesion={}, usuario={}, jti={}, numeroRefresco={}, IP={}", 
                idsession, idUsuario, nuevoJti, (actualRefresco + 1), clientIp);

        if (actualRefresco + 1 == maxRefrescos) {
            log.warn("Último refresco permitido alcanzado: sesion={}, usuario={}, IP={}", 
                    idsession, idUsuario, clientIp);
            revocarTokenSiAplica(refreshClaims);
        }

        String nuevoAccessValue = Base64.getEncoder().encodeToString(nuevoJwtAccess.getBytes(StandardCharsets.UTF_8));
        ResponseCookie nuevaAccessCookie = ResponseCookie.from("Acceso-" + idsession, nuevoAccessValue)
                .httpOnly(true).secure(true).sameSite("Strict").path("/").build();
        response.addHeader("Set-Cookie", nuevaAccessCookie.toString());

        log.debug("Cookie de acceso actualizada: sesion={}", idsession);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new CookieNotFoundException("No se encontró la cookie " + name));
    }

    private String[] decodeAndSplit(String value, String delimiter, int expectedParts, String errorMessage) {
        String decoded = decodeBase64(value);
        String[] parts = decoded.split(delimiter);
        if (parts.length < expectedParts) throw new TokenParseException(errorMessage);
        return parts;
    }

    private String decodeBase64(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new TokenParseException("Error decodificando Base64: " + e.getMessage());
        }
    }

    private Claims parseValidToken(String jwt, String errorMessage) {
        if (!tokenProvider.validateToken(jwt)) throw new TokenValidationException(errorMessage);
        return tokenProvider.parseToken(jwt);
    }

    private void revocarTokenSiAplica(Claims claims) {
        String jti = claims.getId();
        Date exp = claims.getExpiration();
        long ttl = exp.getTime() - System.currentTimeMillis();
        if (jti != null && ttl > 0) {
            tokenRepository.blacklistToken(jti, ttl);
            log.info("Token revocado con jti={} (TTL: {}ms)", jti, ttl);
        }
    }

}
