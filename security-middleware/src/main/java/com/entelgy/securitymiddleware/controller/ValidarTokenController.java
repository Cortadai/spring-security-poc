package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.config.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ValidarTokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/validartoken")
    public ResponseEntity<?> validarToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "motivo", "Token vacío"));
        }

        if (!tokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("valido", false, "motivo", "Token inválido"));
        }

        Claims claims = tokenProvider.parseToken(token);

        return ResponseEntity.ok(Map.of(
                "valido", true,
                "idusuario", claims.get("idusuario", String.class),
                "idaplicacion", claims.get("idaplicacion", String.class),
                "idsession", claims.get("idsession", String.class),
                "roles", claims.get("roles")
        ));
    }
}
