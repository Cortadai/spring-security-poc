package com.entelgy.bank.service;

import com.entelgy.bank.config.BankUserDetailsService;
import com.entelgy.bank.config.TokenProvider;
import com.entelgy.bank.dto.TokenPair;
import com.entelgy.bank.model.Customer;
import com.entelgy.bank.model.LoginRequest;
import com.entelgy.bank.model.LoginResponse;
import com.entelgy.bank.model.RefreshRequest;
import com.entelgy.bank.repository.CustomerRepository;
import com.entelgy.bank.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;

import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final BankUserDetailsService bankUserDetailsService;

    public boolean registerNewUser(Customer customer) {
        // Verifica si ya existe un usuario con ese email
        customerRepository.findByEmail(customer.getEmail())
                .ifPresent(user -> {
                    throw new RuntimeException("User already exists");
                });
        // Cifra la contraseña y guarda el cliente
        String hashPwd = passwordEncoder.encode(customer.getPassword());
        customer.setPassword(hashPwd);
        customer.setCreateDt(new Date(System.currentTimeMillis()));
        Customer savedCustomer = customerRepository.save(customer);
        return savedCustomer.getId() > 0;
    }

    public TokenPair loginExistingUserAndGetTokenPair(LoginRequest loginRequest) {
        TokenPair tokenPair = new TokenPair("", "");
        Authentication authentication = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.username(), loginRequest.password());
        Authentication authenticationResponse = authenticationManager.authenticate(authentication);
        if (authenticationResponse != null && authenticationResponse.isAuthenticated()) {
            tokenPair = tokenProvider.generateTokenPair(authenticationResponse);
        }
        if (!tokenPair.getAccessToken().isEmpty() && !tokenPair.getRefreshToken().isEmpty()) {
            tokenRepository.removeAllTokens(authenticationResponse.getName());
            tokenRepository.storeTokens(authentication.getName(), tokenPair.getAccessToken(),tokenPair.getRefreshToken());
        }
        return tokenPair;
    }

    public void logout() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        tokenRepository.removeAllTokens(userDetails.getUsername());
        SecurityContextHolder.clearContext();
    }

    public ResponseEntity<?> refreshToken(RefreshRequest refreshRequest) {
        String oldRefreshToken = refreshRequest.refreshToken();

        // 1- Validar token
        if(!tokenProvider.validateToken(oldRefreshToken)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid refresh token");
        }

        if(tokenRepository.isRefreshTokenBlacklisted(oldRefreshToken)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("ERROR: refresh token is blacklisted");
        }

        String username = tokenProvider.getUsernameFromToken(oldRefreshToken);
        String storedRefreshToken = tokenRepository.getRefreshToken(username);
        if(storedRefreshToken == null || !storedRefreshToken.equals(oldRefreshToken)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("ERROR: Invalid refresh token");
        }

        // 2- Invalidar oldRefreshToken y access token anteriores
        tokenRepository.removeAllTokens(username);

        // 3- Generar nuevos tokens
        UserDetails userDetails = bankUserDetailsService.loadUserByUsername(username);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        TokenPair newTokenPair = tokenProvider.generateTokenPair(authentication);

        // 4- Guardar nuevos tokens
        tokenRepository.storeTokens(username, newTokenPair.getAccessToken(), newTokenPair.getRefreshToken());

        // 5- Devolver los nuevos tokens
        return ResponseEntity.status(HttpStatus.OK)
                .header(JWT_HEADER, newTokenPair.getAccessToken())
                .header(JWT_HEADER_REFRESH, newTokenPair.getRefreshToken())
                .body(new LoginResponse("Tokens refreshed",
                        newTokenPair.getAccessToken(), newTokenPair.getRefreshToken()));
    }

}
