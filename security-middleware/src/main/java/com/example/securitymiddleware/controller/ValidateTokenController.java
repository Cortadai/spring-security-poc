package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.exception.TokenValidationException;
import com.example.securitymiddleware.service.TokenProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class ValidateTokenController extends BaseController {

    public ValidateTokenController(TokenProvider tokenProvider) {
        super(tokenProvider);
    }

    @PostMapping("/validartoken")
    public ResponseEntity<Map<String, Object>> validarToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new ParameterNotFoundException("Token vacío o nulo");
        }

        log.info("Validando token con sufijo: {}", token.substring(Math.max(0, token.length() - 10)));

        if (!tokenProvider.validateToken(token)) {
            throw new TokenValidationException("Token inválido o expirado");
        }

        Claims claims = tokenProvider.parseToken(token);
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
            log.info("Token con sufijo {} tiene NumeroRefresco={}",
                    token.substring(Math.max(0, token.length() - 10)),
                    claims.get("NumeroRefresco"));
            respuesta.put("NumeroRefresco", claims.get("NumeroRefresco", Integer.class));
        }

        return ResponseEntity.status(HttpStatus.OK).body(respuesta);

    }
}
