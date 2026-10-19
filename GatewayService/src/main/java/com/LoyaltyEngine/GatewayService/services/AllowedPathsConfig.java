package com.LoyaltyEngine.GatewayService.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AllowedPathsConfig {

    @Bean
    public List<String> allowedPaths() {
        return List.of("/api/v1/users/auth", "/api/v1/users/register", "/actuator/**", "/api/v1/rules/getRules", "/fallback/**", "/api/v1/products/category/**", "/api/v1/products/all");
    }

}
