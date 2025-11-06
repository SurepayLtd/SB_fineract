package org.apache.fineract.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.apache.fineract.infrastructure.security.service.JwtTokenUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationFilter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jwtToken = token;
                // Try to decode from Base64 if needed and check for JWT format
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(token);
                    String decoded = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    // JWTs have 3 segments separated by dots
                    if (decoded.split("\\.").length == 3) {
                        jwtToken = decoded;
                    }
                } catch (IllegalArgumentException e) {
                    // Not base64, use as is
                }
                String username = org.apache.fineract.infrastructure.security.service.JwtTokenUtil.getUsernameFromToken(jwtToken);
                List<String> authorities = org.apache.fineract.infrastructure.security.service.JwtTokenUtil.getAuthoritiesFromToken(jwtToken);
                if (username != null) {
                    log.debug("JWT authentication for user: {} with authorities: {}", username, authorities);
                    AppUser appUser = appUserRepository.findAppUserByName(username);
                    if (appUser != null) {
                        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(appUser, null, grantedAuthorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.warn("JWT authentication failed: user not found: {}", username);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
