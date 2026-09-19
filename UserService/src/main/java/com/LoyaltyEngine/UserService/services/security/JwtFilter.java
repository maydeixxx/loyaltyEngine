package com.LoyaltyEngine.UserService.services.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String email = request.getHeader("X-User-Email");

        if (email != null && role != null && !email.isBlank() && !role.isBlank() && userId != null && !userId.isBlank()) {
            try {
                String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority(formattedRole));
                UserSecurity userSecurity = new UserSecurity(email, UUID.fromString(userId));

                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        userSecurity,
                        null,
                        roles
                );
                SecurityContextHolder.getContext().setAuthentication(token);
            } catch (Exception e) {
                log.error("Error filtering request: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
