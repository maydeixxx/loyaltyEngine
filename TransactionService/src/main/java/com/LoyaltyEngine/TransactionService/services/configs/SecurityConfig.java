package com.LoyaltyEngine.TransactionService.services.configs;

import com.LoyaltyEngine.TransactionService.services.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security) {
        return security.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(_ -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowedOrigins(List.of("*"));
                    corsConfiguration.setAllowCredentials(false);
                    return corsConfiguration;
                }))
                .authorizeHttpRequests(request -> request.anyRequest().permitAll())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
