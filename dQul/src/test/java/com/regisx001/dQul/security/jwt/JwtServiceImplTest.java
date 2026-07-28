package com.regisx001.dQul.security.jwt;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.regisx001.dQul.common.domain.User;

class JwtServiceImplTest {

    private static final String BASE64_SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBiYXNlNjQgc2VjcmV0IGtleSBmb3IgSldUIHRlc3RpbmcgcHVycG9zZXM=";
    private static final long EXPIRATION = 3600000L; // 1 hour

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    private User createSampleUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPass")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── generateToken ────────────────────────────────────────────────────

    @Test
    void generateToken_shouldProduceValidToken() {
        User user = createSampleUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3); // header.payload.signature
    }

    // ── extractUsername ──────────────────────────────────────────────────

    @Test
    void extractUsername_shouldReturnSubject() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    // ── extractRoles ─────────────────────────────────────────────────────

    @Test
    void extractRoles_shouldReturnRolesFromToken() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        List<String> roles = jwtService.extractRoles(token);

        assertEquals(1, roles.size());
        assertEquals("USER", roles.get(0));
    }

    // ── isTokenExpired ───────────────────────────────────────────────────

    @Test
    void isTokenExpired_shouldReturnFalseForFreshToken() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenExpired(token));
    }

    // ── isTokenValid ─────────────────────────────────────────────────────

    @Test
    void isTokenValid_shouldReturnTrueForValidTokenAndUser() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        User user = createSampleUser();
        User otherUser = createSampleUser();
        otherUser.setUsername("otheruser");
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    // ── getJwtExpiration ─────────────────────────────────────────────────

    @Test
    void getJwtExpiration_shouldReturnConfiguredValue() {
        assertEquals(EXPIRATION, jwtService.getJwtExpiration());
    }

    // ── edge cases ───────────────────────────────────────────────────────

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() throws InterruptedException {
        // Create a service with a 1ms expiration
        JwtServiceImpl shortLived = new JwtServiceImpl();
        ReflectionTestUtils.setField(shortLived, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(shortLived, "jwtExpiration", 1L);

        User user = createSampleUser();
        String token = shortLived.generateToken(user);

        // Wait for the token to expire
        Thread.sleep(5);

        assertTrue(shortLived.isTokenExpired(token));
        assertFalse(shortLived.isTokenValid(token, user));
    }

    @Test
    void extractUsername_shouldThrowForMalformedToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername("invalid.token.here"));
    }
}
