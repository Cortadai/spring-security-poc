package com.example.bank.exception.entrypoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CustomBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setHeader("bank-error-reason", "Authentication failed");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        String message = (authException != null && authException.getMessage() != null)
                ? authException.getMessage()
                : "Unauthorized";

        String originalPath = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        String path = originalPath != null ? originalPath : request.getRequestURI();

        String jsonResponse = String.format(
                "{\"timestamp\": \"%s\", " +
                        "\"status\": %d, " +
                        "\"error\": \"%s\", " +
                        "\"message\": \"%s\", " +
                        "\"path\": \"%s\"}",
                LocalDateTime.now().format(FORMATTER),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                path
        );

        log.warn("Unauthorized access from {} to {}: {}", request.getRemoteAddr(), path, message);
        response.getWriter().write(jsonResponse);
    }
}
