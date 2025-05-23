package com.example.bank.service;

import com.example.bank.config.BankUserDetailsService;
import com.example.bank.config.TokenProvider;
import com.example.bank.dto.TokenPair;
import com.example.bank.exception.InvalidTokenException;
import com.example.bank.exception.TokenBlacklistedException;
import com.example.bank.exception.TokenMismatchException;
import com.example.bank.model.Customer;
import com.example.bank.model.LoginRequest;
import com.example.bank.model.LoginResponse;
import com.example.bank.model.RefreshRequest;
import com.example.bank.repository.CustomerRepository;
import com.example.bank.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static com.example.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.example.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@Slf4j
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
                    log.error("User already exists for email: {}", customer.getEmail());
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
        log.info("Attempting to logOut  for user: {}", userDetails.getUsername());
        tokenRepository.removeAllTokens(userDetails.getUsername());
        SecurityContextHolder.clearContext();
    }

    public ResponseEntity<?> refreshToken(RefreshRequest refreshRequest) {
        String oldRefreshToken = refreshRequest.refreshToken();
        String username = tokenProvider.getUsernameFromToken(oldRefreshToken);
        log.info("Refreshing TokenPair for user: {}", username);

        // 1- Validar token
        log.info("Validating refresh token for user: {}", username);
        if (!tokenProvider.validateToken(oldRefreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        // 2- Validar blacklist
        log.info("Checking if refresh token is blacklisted for user: {}", username);
        if (tokenRepository.isRefreshTokenBlacklisted(oldRefreshToken)) {
            throw new TokenBlacklistedException("Refresh token is blacklisted");
        }

        // 3- Validar si coincide con el token almacenado
        log.info("Checking if refresh token is in DB for user: {}", username);
        String storedRefreshToken = tokenRepository.getRefreshToken(username);
        if (storedRefreshToken == null || !storedRefreshToken.equals(oldRefreshToken)) {
            throw new TokenMismatchException("Refresh token does not match stored token");
        }

        // 4- Invalidar tokens anteriores
        log.info("Remove all current tokens for user: {}", username);
        tokenRepository.removeAllTokens(username);

        // 5- Generar nuevos tokens
        log.info("Generating new tokens for user: {}", username);
        UserDetails userDetails = bankUserDetailsService.loadUserByUsername(username);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        TokenPair newTokenPair = tokenProvider.generateTokenPair(authentication);

        // 6- Guardar nuevos tokens
        log.info("Storing new tokens in DB for user: {}", username);
        tokenRepository.storeTokens(username, newTokenPair.getAccessToken(), newTokenPair.getRefreshToken());

        // 7- Devolver nuevos tokens
        log.info("Returning new tokens for user: {}", username);
        return ResponseEntity.status(HttpStatus.OK)
                .header(JWT_HEADER, newTokenPair.getAccessToken())
                .header(JWT_HEADER_REFRESH, newTokenPair.getRefreshToken())
                .body(new LoginResponse("Tokens refreshed",
                        newTokenPair.getAccessToken(), newTokenPair.getRefreshToken()));
    }

}
