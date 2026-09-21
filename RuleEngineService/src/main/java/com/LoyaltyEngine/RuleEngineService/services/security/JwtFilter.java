package com.LoyaltyEngine.RuleEngineService.services.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String email = request.getHeader("X-User-Email");
        String role = request.getHeader("X-User-Role");

        if (email != null && role != null && !email.isBlank() && !role.isBlank()) {
            String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority(formattedRole));

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    roles
            );
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }
}
