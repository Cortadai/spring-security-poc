package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ValidarTokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/validartoken")
    public ResponseEntity<?> validarToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        log.info("estamos validando este token: " + token.substring(token.length() - 10));

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "motivo", "Token vacío"));
        }

        if (!tokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("valido", false, "motivo", "Token inválido"));
        }

        Claims claims = tokenProvider.parseToken(token);

        // Identificamos tipo de token
        String tipo = claims.get("typ", String.class);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("valido", true);
        respuesta.put("typ", tipo);
        respuesta.put("idusuario", claims.get("idusuario", String.class));
        respuesta.put("idaplicacion", claims.get("idaplicacion", String.class));
        respuesta.put("idsession", claims.get("idsession", String.class));
        respuesta.put("roles", claims.get("roles"));
        respuesta.put("exp", claims.getExpiration().getTime());

        if ("acceso".equals(tipo)) {
            log.info("el token: " + token.substring(token.length() - 10) + "tiene este numero de refrescos: " + claims.get("NumeroRefresco"));
            respuesta.put("NumeroRefresco", claims.get("NumeroRefresco", Integer.class));
        }

        return ResponseEntity.ok(respuesta);
    }
}

