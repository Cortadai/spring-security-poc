package com.example.bank.exception.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");

        String message = (accessDeniedException != null && accessDeniedException.getMessage() != null) ?
                accessDeniedException.getMessage() : "Authorization failed";

        String originalPath = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        String path = originalPath != null ? originalPath : request.getRequestURI();

        String jsonResponse =
                String.format("{\"timestamp\": \"%s\", " +
                                "\"status\": %d, " +
                                "\"error\": \"%s\", " +
                                "\"message\": \"%s\", " +
                                "\"path\": \"%s\"}",
                        LocalDateTime.now().format(FORMATTER),
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        message,
                        path
        );

        log.warn("Unauthorized access from {} to {}: {}", request.getRemoteAddr(), path, message);
        response.getWriter().write(jsonResponse);
    }

}
