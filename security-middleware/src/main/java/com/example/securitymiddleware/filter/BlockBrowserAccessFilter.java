package com.example.securitymiddleware.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BlockBrowserAccessFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String secFetchMode = request.getHeader("Sec-Fetch-Mode");

        boolean probableBrowserRequest = origin != null || referer != null || secFetchMode != null;

        if (probableBrowserRequest) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Acceso no permitido");
            return;
        }

        filterChain.doFilter(request, response);
    }

}
