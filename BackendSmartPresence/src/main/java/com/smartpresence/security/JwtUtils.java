package com.smartpresence.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilitaires pour la génération, la validation et le parsing des jetons JWT.
 *
 * @since 0.0.1
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${app.security.jwt.secret:Y2hhbmdlLW1lLXNlY3JldC1rZXktZm9yLXNtYXJ0cHJlc2VuY2UtMjU2LWJpdHMtbG9uZy1zZWN1cmU}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs; // 1h

    @Value("${app.security.jwt.refresh-expiration-ms:86400000}")
    private long jwtRefreshExpirationMs; // 24h

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("userId", userPrincipal.getId().toString());
        claims.put("nom", userPrincipal.getNom());
        claims.put("prenom", userPrincipal.getPrenom());

        return generateToken(claims, userPrincipal.getUsername(), jwtExpirationMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return generateToken(new HashMap<>(), userPrincipal.getUsername(), jwtRefreshExpirationMs);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            log.error("Jeton JWT invalide ou expiré : {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Chaîne JWT vide : {}", ex.getMessage());
        }
        return false;
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
