package com.entelgy.bank.filter;

import com.entelgy.bank.config.BankUserDetailsService;
import com.entelgy.bank.config.TokenProvider;
import com.entelgy.bank.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER;

@RequiredArgsConstructor
public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final BankUserDetailsService bankUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(JWT_HEADER);
        if (jwt != null) {
            try{
                // Paso 1 - Firma y expiracion
                if (!tokenProvider.validateToken(jwt)) {
                    throw new BadCredentialsException("Token inválido o expirado.");
                }
                // Paso 2: Validar blacklist
                if (tokenRepository.isAccessTokenBlacklisted(jwt)) {
                    throw new BadCredentialsException("Token en blacklist.");
                }
                // Paso 3: Verificar que coincide con el almacenado
                Claims claims = tokenProvider.parseToken(jwt);
                String username = String.valueOf(claims.get("username"));
                String storedToken = tokenRepository.getAccessToken(username);
                if (storedToken == null || !storedToken.equals(jwt)) {
                    throw new BadCredentialsException("Token no coincide con el almacenado.");
                }
                // Paso 4: Ha ido bien, guardamos en el contexto
                UserDetails userDetails = bankUserDetailsService.loadUserByUsername(username);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                throw new BadCredentialsException("Fallo en la validación del token: %s".formatted(e.getMessage()));
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getServletPath().equals("/user");
    }

}
