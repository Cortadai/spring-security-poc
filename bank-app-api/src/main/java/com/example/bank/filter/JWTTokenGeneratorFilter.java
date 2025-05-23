package com.example.bank.filter;

import com.example.bank.config.TokenProvider;
import com.example.bank.dto.TokenPair;
import com.example.bank.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.example.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@RequiredArgsConstructor
public class JWTTokenGeneratorFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            TokenPair tokenPair = tokenProvider.generateTokenPair(authentication);
            if (!tokenPair.getAccessToken().isEmpty() && !tokenPair.getRefreshToken().isEmpty()) {
                tokenRepository.removeAllTokens(authentication.getName());
                tokenRepository.storeTokens(authentication.getName(), tokenPair.getAccessToken(),tokenPair.getRefreshToken());
            }
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
