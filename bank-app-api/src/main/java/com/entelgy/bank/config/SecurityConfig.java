package com.entelgy.bank.config;

import com.entelgy.bank.exception.CustomAccessDeniedHandler;
import com.entelgy.bank.filter.JwtCookieAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

import static com.entelgy.bank.constants.ApplicationConstants.*;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.cors(corsConfig -> corsConfig.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Collections.singletonList("http://localhost:4200")); //"https://spa.domain.es"
                config.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
                config.setAllowCredentials(true);
                // allowedHeaders = lo que Angular puede enviar
                // exposedHeaders = lo que Angular puede leer
                config.setAllowedHeaders(Arrays.asList(CONTENT, XIDSESSION, XCSRFTOKEN, XCERTAUTH));
                config.setExposedHeaders(Collections.singletonList(XIDSESSION));  //"Authorization" para version 2
                config.setMaxAge(3600L);
                return config;
            }
        }));
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(csrfConfig -> csrfConfig.disable());
        http.requiresChannel(rcc -> rcc.anyRequest().requiresInsecure());       //.requiresSecure()
        http.addFilterBefore(jwtCookieAuthenticationFilter, BasicAuthenticationFilter.class);
        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/myAccount").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/myBalance").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/myLoans").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/myCards").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/getClaims").authenticated()
                .requestMatchers("/refresh").authenticated()
                .requestMatchers("/expires").authenticated()
                .requestMatchers("/getSessionState").authenticated()
                .requestMatchers("/notices", "/contact", "/logInEnd", "/logOff").permitAll()
        );
        http.formLogin(flc -> flc.disable());
        http.httpBasic(hbc -> hbc.disable());
        http.logout(loc -> loc.disable()); // use LogOff instead LogIn in angular and controllers to avoid problems
        http.exceptionHandling(ehc-> ehc.accessDeniedHandler(new CustomAccessDeniedHandler()));
        return http.build();
    }

}
