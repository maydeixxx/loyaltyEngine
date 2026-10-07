package com.LoyaltyEngine.ProductService.services.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class HeadersAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userRole = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        if (userRole != null && userId != null && !userRole.isBlank() && !userId.isBlank()) {
            String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;
            List<SimpleGrantedAuthority> simpleGrantedAuthority = List.of(new SimpleGrantedAuthority(formattedRole));

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    new UserSecurity(UUID.fromString(userId)),
                    null,
                    simpleGrantedAuthority
            );
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }
}
