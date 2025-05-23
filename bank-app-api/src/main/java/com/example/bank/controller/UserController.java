package com.example.bank.controller;

import com.example.bank.dto.TokenPair;
import com.example.bank.model.Customer;
import com.example.bank.model.LoginRequest;
import com.example.bank.model.LoginResponse;
import com.example.bank.model.RefreshRequest;
import com.example.bank.repository.CustomerRepository;
import com.example.bank.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static com.example.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.example.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final CustomerRepository customerRepository;
    private final Environment env;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody Customer customer) {
        try {
            boolean registered = authService.registerNewUser(customer);
            if (registered) {
                log.info("User registered successfully for email: {}", customer.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).
                        body("Given user details are successfully registered");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                        body("User registration failed");
            }
        } catch (Exception ex) {
            log.error("An exception occurred: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                    body("An exception occurred: " + ex.getMessage());
        }
    }

    @RequestMapping("/user")
    public Customer getUserDetailsAfterLogin(Authentication authentication) {
        log.info("Attempting to logIn via BasicAuth for user: {}", authentication.getName());
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(authentication.getName());
        return optionalCustomer.orElse(null);
    }

    @PostMapping("/apiLogin")
    public ResponseEntity<LoginResponse> apiLogin(@RequestBody LoginRequest loginRequest) {
        log.info("Attempting to logIn via API for user: {}", loginRequest.username());
        TokenPair tokenPair = authService.loginExistingUserAndGetTokenPair(loginRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .header(JWT_HEADER, tokenPair.getAccessToken())
                .header(JWT_HEADER_REFRESH, tokenPair.getRefreshToken())
                .body(new LoginResponse(HttpStatus.OK.getReasonPhrase(),
                        tokenPair.getAccessToken(), tokenPair.getRefreshToken()));
    }

    @PostMapping("/apiLogout")
    public ResponseEntity<String> apiLogout() {
        authService.logout();
        return ResponseEntity.status(HttpStatus.OK).body("Logout successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> apiRefreshToken(@RequestBody RefreshRequest refreshRequest) {
        return authService.refreshToken(refreshRequest);
    }

}
