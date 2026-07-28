package com.regisx001.dQul.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.regisx001.dQul.authentication.AuthenticationRequest;
import com.regisx001.dQul.authentication.AuthenticationResponse;
import com.regisx001.dQul.authentication.RegisterRequest;
import com.regisx001.dQul.common.User;
import com.regisx001.dQul.common.UserRepository;
import com.regisx001.dQul.security.JwtService;
import com.regisx001.dQul.common.UserService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    private AuthenticationServiceImpl authService;

    private User sampleUser;
    private UUID sampleId;
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.testToken";

    @BeforeEach
    void setUp() {
        authService = new AuthenticationServiceImpl(userService, jwtService,
                passwordEncoder, userRepository);
        sampleId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("{bcrypt}encodedPass")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("rawPassword")
                .fullName("New User")
                .role("USER")
                .build();

        when(passwordEncoder.encode("rawPassword")).thenReturn("{bcrypt}encodedPass");
        when(userService.createUser("newuser", "new@example.com",
                "{bcrypt}encodedPass", "New User", "USER"))
                .thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn(TEST_TOKEN);
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        AuthenticationResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(86400000L, response.getExpiresIn());
        assertEquals(sampleId, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("USER", response.getRole());

        verify(passwordEncoder).encode("rawPassword");
        verify(userService).createUser("newuser", "new@example.com",
                "{bcrypt}encodedPass", "New User", "USER");
        verify(jwtService).generateToken(sampleUser);
    }

    // ── authenticate (by username) ───────────────────────────────────────

    @Test
    void authenticate_withUsername_shouldReturnToken() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .login("testuser")
                .password("rawPassword")
                .build();

        when(userService.getUserByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", sampleUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn(TEST_TOKEN);
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthenticationResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(sampleId, response.getUserId());

        verify(userService).getUserByUsername("testuser");
        verify(passwordEncoder).matches("rawPassword", sampleUser.getPasswordHash());
        verify(jwtService).generateToken(sampleUser);
        verify(userRepository).save(any(User.class));
        assertNotNull(sampleUser.getLastLoginAt());
    }

    // ── authenticate (by email) ──────────────────────────────────────────

    @Test
    void authenticate_withEmail_shouldReturnToken() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .login("test@example.com")
                .password("rawPassword")
                .build();

        when(userService.getUserByEmail("test@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", sampleUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn(TEST_TOKEN);
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthenticationResponse response = authService.authenticate(request);

        assertEquals(TEST_TOKEN, response.getToken());
        verify(userService).getUserByEmail("test@example.com");
        verify(userService, never()).getUserByUsername(any());
    }

    // ── authenticate — invalid password ──────────────────────────────────

    @Test
    void authenticate_shouldThrowOnWrongPassword() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .login("testuser")
                .password("wrongPassword")
                .build();

        when(userService.getUserByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash()))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.authenticate(request));
    }

    // ── authenticate — deactivated user ──────────────────────────────────

    @Test
    void authenticate_shouldThrowWhenUserDeactivated() {
        sampleUser.setActive(false);
        AuthenticationRequest request = AuthenticationRequest.builder()
                .login("testuser")
                .password("rawPassword")
                .build();

        when(userService.getUserByUsername("testuser")).thenReturn(sampleUser);

        assertThrows(IllegalStateException.class,
                () -> authService.authenticate(request));
    }

    // ── verifyToken ──────────────────────────────────────────────────────

    @Test
    void verifyToken_shouldReturnTrueWhenNotExpired() {
        when(jwtService.isTokenExpired(TEST_TOKEN)).thenReturn(false);

        assertTrue(authService.verifyToken(TEST_TOKEN));
    }

    @Test
    void verifyToken_shouldReturnFalseWhenExpired() {
        when(jwtService.isTokenExpired(TEST_TOKEN)).thenReturn(true);

        assertFalse(authService.verifyToken(TEST_TOKEN));
    }

    @Test
    void verifyToken_shouldReturnFalseOnException() {
        when(jwtService.isTokenExpired(TEST_TOKEN)).thenThrow(new RuntimeException("bad token"));

        assertFalse(authService.verifyToken(TEST_TOKEN));
    }
}
