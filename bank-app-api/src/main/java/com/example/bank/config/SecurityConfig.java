package com.example.bank.config;

import com.example.bank.exception.handler.CustomAccessDeniedHandler;
import com.example.bank.exception.entrypoint.CustomBasicAuthenticationEntryPoint;
import com.example.bank.filter.CsrfCookieFilter;
import com.example.bank.filter.JWTTokenGeneratorFilter;
import com.example.bank.filter.JWTTokenValidatorFilter;
import com.example.bank.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

import static com.example.bank.constants.ApplicationConstants.JWT_HEADER;
import static com.example.bank.constants.ApplicationConstants.JWT_HEADER_REFRESH;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final BankUserDetailsService bankUserDetailsService;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, TokenRepository tokenRepository) throws Exception {
        CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
        http.cors(corsConfig -> corsConfig.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
                config.setAllowedMethods(Collections.singletonList("*"));
                config.setAllowCredentials(true);
                config.setAllowedHeaders(Collections.singletonList("*"));
                config.setExposedHeaders(Arrays.asList(JWT_HEADER, JWT_HEADER_REFRESH));
                config.setMaxAge(3600L);
                return config;
            }
        }));
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(csrfConfig -> csrfConfig
                .csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
                .ignoringRequestMatchers("/contact", "/register", "/apiLogin", "/apiLogout", "/refresh")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        // devolver el token CSRF solo tras una autenticación exitosa
        http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        // emitir el JWT tras una autenticación exitosa, para que el cliente pueda usarlo en peticiones posteriores
        http.addFilterAfter(new JWTTokenGeneratorFilter(tokenProvider, tokenRepository), BasicAuthenticationFilter.class);
        // validar tokens antes de que cualquier otro mecanismo (como basic auth) entre en acción
        http.addFilterBefore(
                new JWTTokenValidatorFilter(tokenProvider, tokenRepository, bankUserDetailsService),
                BasicAuthenticationFilter.class
        );        http.requiresChannel(rcc -> rcc.anyRequest().requiresInsecure()); // Only HTTP
        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/myAccount").hasRole("USER") // we don't use prefix ROLE_USER, only USER
                .requestMatchers("/myBalance").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/myLoans").hasRole("USER")
                .requestMatchers("/myCards").hasRole("USER")
                .requestMatchers("/user").authenticated()
                .requestMatchers("/apiLogout").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/refresh").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/notices", "/contact", "/register", "/apiLogin").permitAll()
        );
        http.formLogin(flc -> flc.disable());
        http.httpBasic(hbc -> hbc.authenticationEntryPoint(new CustomBasicAuthenticationEntryPoint()));
        http.exceptionHandling(ehc-> ehc.accessDeniedHandler(new CustomAccessDeniedHandler()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        BankUsernamePwdAuthenticationProvider authenticationProvider =
                new BankUsernamePwdAuthenticationProvider(userDetailsService, passwordEncoder);
        ProviderManager providerManager = new ProviderManager(authenticationProvider);
        providerManager.setEraseCredentialsAfterAuthentication(false);
        return providerManager;
    }

}
