package com.LoyaltyEngine.UserService.services.security;

import com.LoyaltyEngine.UserService.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    @Value("${jwt.lifetime}")
    private Duration lifetime;

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret-key}") String signKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(signKey));
    }

    public String generateJwtToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("Role", "ROLE_" + user.getRole());
        claims.put("User_id", user.getId());

        Date issuedAt = new Date();
        Date expireTime = new Date(issuedAt.getTime() + lifetime.toMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .expiration(expireTime)
                .issuedAt(issuedAt)
                .signWith(secretKey)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
}
