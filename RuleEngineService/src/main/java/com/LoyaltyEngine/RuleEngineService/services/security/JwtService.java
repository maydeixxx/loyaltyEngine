package com.LoyaltyEngine.RuleEngineService.services.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret-key}") String signingKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingKey));
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role").toString();
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
}
