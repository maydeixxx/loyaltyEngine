package com.LoyaltyEngine.GatewayService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
@Slf4j
public class JwtService {
    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret-key}") String jwtKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error parsing claims: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
