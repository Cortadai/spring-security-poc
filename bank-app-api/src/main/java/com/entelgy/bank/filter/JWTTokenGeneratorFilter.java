package com.entelgy.bank.filter;

import com.entelgy.bank.config.TokenProvider;
import com.entelgy.bank.dto.TokenPair;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@RequiredArgsConstructor
public class JWTTokenGeneratorFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            TokenPair tokenPair = tokenProvider.generateTokenPair(authentication);
            response.setHeader(JWT_HEADER, tokenPair.getAccessToken());
            response.setHeader(JWT_HEADER_REFRESH, tokenPair.getRefreshToken());
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Solo ejecutar si la ruta es exactamente /user. Para cualquier otra ruta, no hacer nada
        return !request.getServletPath().equals("/user");
    }

}
