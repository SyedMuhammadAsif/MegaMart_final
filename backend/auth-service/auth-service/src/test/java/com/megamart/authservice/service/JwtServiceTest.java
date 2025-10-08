package com.megamart.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "mySecretKeyForTestingPurposesOnly1234567890";
    private final long jwtExpiration = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", jwtExpiration);
    }

    @Test
    void generateToken_Success() {
        String token = jwtService.generateToken("test@example.com", "USER", 1L);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        Claims claims = extractClaims(token);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("role", String.class));
        assertEquals(1L, claims.get("userId", Long.class));
    }

    @Test
    void extractEmail_Success() {
        String token = jwtService.generateToken("test@example.com", "USER", 1L);

        String email = jwtService.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void extractRole_Success() {
        String token = jwtService.generateToken("test@example.com", "ADMIN", 1L);

        String role = jwtService.extractRole(token);

        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void extractUserId_Success() {
        String token = jwtService.generateToken("test@example.com", "USER", 123L);

        Long userId = jwtService.extractUserId(token);

        assertEquals(123L, userId);
    }

    @Test
    void validateToken_ValidToken() {
        String token = jwtService.generateToken("test@example.com", "USER", 1L);

        boolean isValid = jwtService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_ExpiredToken() {
        // Create expired token
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();

        boolean isValid = jwtService.validateToken(expiredToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_BlacklistedToken() {
        String token = jwtService.generateToken("test@example.com", "USER", 1L);
        jwtService.blacklistToken(token);

        boolean isValid = jwtService.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void validateToken_MalformedToken() {
        boolean isValid = jwtService.validateToken("invalid.token.format");

        assertFalse(isValid);
    }

    @Test
    void blacklistToken_Success() {
        String token = "test-token";

        jwtService.blacklistToken(token);

        assertTrue(jwtService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_NotBlacklisted() {
        String token = "test-token";

        boolean isBlacklisted = jwtService.isTokenBlacklisted(token);

        assertFalse(isBlacklisted);
    }

    @Test
    void isTokenExpired_NotExpired() {
        String token = jwtService.generateToken("test@example.com", "USER", 1L);

        boolean isExpired = jwtService.isTokenExpired(token);

        assertFalse(isExpired);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}