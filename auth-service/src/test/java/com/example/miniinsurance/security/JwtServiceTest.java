package com.example.miniinsurance.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!!";
    private static final String EMAIL = "ana@example.com";

    private final JwtService jwtService = new JwtService(new JwtConfig(SECRET, 60_000L));

    @Test
    void generatedTokenIsValidAndCarriesEmailAsSubject() {
        String token = jwtService.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(EMAIL);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiredService = new JwtService(new JwtConfig(SECRET, -1_000L));

        String token = expiredService.generateToken(EMAIL);

        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService(new JwtConfig(
                "another-secret-key-that-is-also-32-bytes-long!!", 60_000L));

        String foreign = other.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(foreign)).isFalse();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
    }
}
