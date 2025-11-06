package org.apache.fineract.infrastructure.security.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

    public static String generateToken(String username, Collection<String> authorities) {
        return Jwts.builder()
                .setSubject(username)
                .claim("authorities", authorities)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public static List<String> getAuthoritiesFromToken(String token) {
        Object authorities = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("authorities");
        if (authorities instanceof List<?>) {
            return ((List<?>) authorities).stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
