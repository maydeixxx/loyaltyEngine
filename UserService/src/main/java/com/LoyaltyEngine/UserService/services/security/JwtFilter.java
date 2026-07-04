package com.LoyaltyEngine.UserService.services.security;

import com.LoyaltyEngine.UserService.exceptions.JwtCheckingException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String jwt = null;
        String email = null;

        if (header != null && header.startsWith("Bearer ")) {
            jwt = header.replace("Bearer ", "");
            try {
                email = jwtService.getEmailFromToken(jwt);
            } catch (Exception e) {
                throw new JwtCheckingException(String.format("Error checking jwt: {}", e.getMessage()));
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority(jwtService.getRoleFromToken(jwt)))
            );
            SecurityContextHolder.getContext().setAuthentication(token);
        }

        filterChain.doFilter(request, response);
    }
}
