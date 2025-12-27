package com.sararahmani.site.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Secret doit avoir au moins 32 caracteres pour HS256
        String secret = "testSecretKeyForUnitTestingPurposes123456789";
        long expiration = 86400000; // 1 jour
        jwtService = new JwtService(secret, expiration);
    }

    @Test
    @DisplayName("Generer un token valide")
    void generateToken_Success() {
        String token = jwtService.generateToken("test@example.com", Map.of("role", "PATIENT"));

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // Format JWT: header.payload.signature
    }

    @Test
    @DisplayName("Extraire le subject du token")
    void extractSubject_Success() {
        String email = "test@example.com";
        String token = jwtService.generateToken(email, Map.of("role", "PATIENT"));

        String subject = jwtService.extractSubject(token);

        assertThat(subject).isEqualTo(email);
    }

    @Test
    @DisplayName("Token valide retourne true")
    void isTokenValid_ValidToken() {
        String token = jwtService.generateToken("test@example.com", Map.of("role", "PATIENT"));

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Token invalide retourne false")
    void isTokenValid_InvalidToken() {
        boolean isValid = jwtService.isTokenValid("invalid.token.here");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Token null retourne false")
    void isTokenValid_NullToken() {
        boolean isValid = jwtService.isTokenValid(null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Token modifie retourne false")
    void isTokenValid_TamperedToken() {
        String token = jwtService.generateToken("test@example.com", Map.of("role", "PATIENT"));
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        boolean isValid = jwtService.isTokenValid(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Token avec claims personnalises")
    void generateToken_WithCustomClaims() {
        Map<String, Object> claims = Map.of(
                "role", "ADMIN",
                "customClaim", "customValue"
        );

        String token = jwtService.generateToken("admin@example.com", claims);

        assertThat(token).isNotNull();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("admin@example.com");
    }
}
