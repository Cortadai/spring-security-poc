package com.entelgy.bank.controller;

import com.entelgy.bank.dto.TokenPair;
import com.entelgy.bank.dto.UserDto;
import com.entelgy.bank.model.Customer;
import com.entelgy.bank.model.LoginRequest;
import com.entelgy.bank.model.LoginResponse;
import com.entelgy.bank.model.RefreshRequest;
import com.entelgy.bank.repository.CustomerRepository;
import com.entelgy.bank.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.entelgy.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final CustomerRepository customerRepository;
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

    @GetMapping("/userinfo")
    public ResponseEntity<UserDto> getUserInfo(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = auth.getName();
        List<String> roles = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        UserDto userDto = new UserDto();
        userDto.setId(1);
        userDto.setEmail(email);
        userDto.setRole(roles.contains("ADMIN") ? "ADMIN" : "USER");
        userDto.setName(email.split("@")[0]); // simulado
        userDto.setMobileNumber("1234567890");
        return ResponseEntity.ok(userDto);
    }

}
