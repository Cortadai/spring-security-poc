package com.entelgy.bank.service;

import com.entelgy.bank.config.TokenProvider;
import com.entelgy.bank.dto.TokenPair;
import com.entelgy.bank.model.Customer;
import com.entelgy.bank.model.LoginRequest;
import com.entelgy.bank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

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
        return tokenPair;
    }

}
