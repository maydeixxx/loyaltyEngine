package com.LoyaltyEngine.GatewayService.services;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;


@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver keyResolver() {
        return exchange -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            if (headers.containsHeader("X-USER-ID")) {
                String userIdHeader = headers.getFirst("X-USER-ID");
                if (userIdHeader != null) {
                    return Mono.just(userIdHeader);
                }
            }

            String ip;
            var address = exchange.getRequest().getRemoteAddress();
            if (address != null && address.getAddress() != null && address.getAddress().getHostAddress() != null) {
                ip = address.getAddress().getHostAddress();
            } else {
                ip = "anon";
            }

            return Mono.just(ip);
        };
    }

}
