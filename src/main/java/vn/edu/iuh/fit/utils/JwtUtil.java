package vn.edu.iuh.fit.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.auths.UserPrincipal;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtil {
    private final Key secretKey;
    public final long accessTokenExpiration;   // thời gian sống của Access Token
    private final long refreshTokenExpiration;  // thời gian sống của Refresh Token

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateToken(String userId, String roleName){
        return Jwts.builder()
                .subject(userId)
                .claim("role", roleName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }
    //Refresh token không cần lưu role
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    public String getPhoneFromToken(org.springframework.security.oauth2.jwt.Jwt jwtToken) {
        return jwtToken.getSubject();
    }
    public boolean isTokenExpired(org.springframework.security.oauth2.jwt.Jwt jwtToken) {
        return Objects.requireNonNull(jwtToken.getExpiresAt()).isBefore(Instant.now());
    }
    public boolean isTokenValid(org.springframework.security.oauth2.jwt.Jwt jwtToken, UserPrincipal userPrincipal) {
        return !isTokenExpired(jwtToken) &&
                userPrincipal.isEnabled() &&
                userPrincipal.getUsername().equals(getPhoneFromToken(jwtToken));
    }
    public LocalDateTime generateExpirationDate() {
        return LocalDateTime.now().plus(7, ChronoUnit.DAYS);
    }
    public Instant generateRefreshExpirationDate() {
        return Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS);
    }

    public long getExpiration() {
        return 3600_000; // 1 hour in milliseconds
    }
}
