package com.example.bank.exception.entrypoint;

import com.example.bank.exception.MiddlewareException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> error = new HashMap<>();
        error.put("status", 401);
        error.put("error", "No autorizado");

        if (authException instanceof MiddlewareException) {
            error.put("mensaje", authException.getMessage()); // mensaje personalizado
        } else {
            error.put("mensaje", "El token es inválido o ha caducado");
        }

        response.getWriter().write(mapper.writeValueAsString(error));
    }

}
