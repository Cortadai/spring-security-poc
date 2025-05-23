package com.example.securitymiddleware.config;

import com.example.securitymiddleware.filter.BlockBrowserAccessFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

import static com.example.securitymiddleware.constants.ApplicationConstants.*;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private final BlockBrowserAccessFilter blockBrowserAccessFilter;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.cors(corsConfig -> corsConfig.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();
                // permitimos el fake-sso y el backend-SPA
                config.setAllowedOrigins(Arrays.asList("http://localhost:9999", "http://localhost:8888")); //"https://spa.domain.es"
                config.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
                config.setAllowCredentials(true);
                config.setAllowedHeaders(Arrays.asList(CONTENT, XIDSESSION, XCERTAUTH));
                config.setExposedHeaders(Collections.singletonList(AUTHORIZATION));
                config.setMaxAge(3600L);
                return config;
            }
        }));
        http.addFilterBefore(blockBrowserAccessFilter, UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(csrfConfig -> csrfConfig.disable());
        http.requiresChannel(rcc -> rcc.anyRequest().requiresInsecure());       //.requiresSecure()
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(
                        "/loginBegin", "/login2End", "/logoff2",
                        "/obtenerclaims2", "/validartoken",
                        "/refresco2", "/expira2", "/estadosession"
                )
                // Permitir acceso libre, pero mantener paso por filtros de seguridad (BlockBrowserAccessFilter)
                .access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(true))
        );
        http.formLogin(flc -> flc.disable());
        http.httpBasic(hbc -> hbc.disable());
        http.logout(loc -> loc.disable()); // use LogOff instead LogIn in angular and controllers to avoid problems
        return http.build();
    }

}
