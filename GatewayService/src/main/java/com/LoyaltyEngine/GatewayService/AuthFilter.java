package com.LoyaltyEngine.GatewayService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {
    private final JwtService jwtService;


    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        try {
            String path = exchange.getRequest().getURI().getPath();

            if (path.equals("/api/v1/users/auth") || path.equals("/api/v1/users/register"))
                return chain.filter(exchange);

            ServerHttpRequest request = exchange.getRequest();
            String authorizationHeader;
            String jwtToken;

            if (request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null) {
                authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            if (authorizationHeader != null && !authorizationHeader.isBlank() && authorizationHeader.startsWith("Bearer ")) {
                jwtToken = authorizationHeader.substring(7);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            Claims claimsFromToken = jwtService.getClaimsFromToken(jwtToken);
            final String email = claimsFromToken.getSubject();
            final Object userId = claimsFromToken.get("User_id");
            final Object role = claimsFromToken.get("Role");

            if (email == null || userId == null || role == null) {
                log.error("Got null vars from claims");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = request.mutate().headers(httpHeaders -> {
                httpHeaders.set("X-User-Email", email);
                httpHeaders.set("X-User-Role", role.toString());
                httpHeaders.set("X-User-Id", userId.toString());
            }).build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (MalformedJwtException | SignatureException e) {
            log.warn("Invalid JWT signature or structure: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
