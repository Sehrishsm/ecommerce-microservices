package com.ecommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final byte[] KEY_BYTES = new byte[32];

    private JwtService createJwtService() {
        String secret = Base64.getEncoder()
                .encodeToString(KEY_BYTES);

        return new JwtService(secret, 900_000);
    }

    private String createToken(Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(KEY_BYTES);
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("34")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = createJwtService();

        String expiredToken = createToken(
                Instant.now().minusSeconds(60));

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void acceptsValidToken() {
        JwtService jwtService = createJwtService();

        String validToken = createToken(
                Instant.now().plusSeconds(300));

        assertTrue(jwtService.isTokenValid(validToken));
    }

    @Test
    void rejectsMalformedToken() {
        JwtService jwtService = createJwtService();

        assertFalse(jwtService.isTokenValid(
                "invalid.token.value"));
    }
}