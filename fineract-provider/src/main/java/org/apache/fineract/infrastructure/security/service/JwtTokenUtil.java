package org.apache.fineract.infrastructure.security.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenUtil {
    private static String secret;
    private static long expirationTime;
    private static Key key;

    @Value("${fineract.security.jwt.secret}")
    private String secretProperty;
    @Value("${fineract.security.jwt.expiration:86400000}")
    private long expirationProperty;

    @PostConstruct
    public void init() {
        secret = secretProperty;
        expirationTime = expirationProperty;
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public static String generateToken(String username, Long userId, Collection<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public static Long getUserIdFromToken(String token) {
        Object userId = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        try {
            return Long.parseLong(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> getRolesFromToken(String token) {
        Object roles = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
