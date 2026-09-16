package com.LoyaltyEngine.GatewayService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security) {
        return security.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(_ -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of("*"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(false);
                    corsConfiguration.setAllowedMethods(List.of("GET", "PUT", "PATCH", "POST", "DELETE", "OPTIONS"));
                    return corsConfiguration;
                }))
                .authorizeHttpRequests(request -> request.anyRequest().permitAll())
                .build();
    }

}
