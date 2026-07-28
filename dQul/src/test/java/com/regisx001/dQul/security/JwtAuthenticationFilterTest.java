package com.regisx001.dQul.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.regisx001.dQul.common.User;
import com.regisx001.dQul.common.UserRepository;
import com.regisx001.dQul.security.JwtService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    private User sampleUser;
    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(handlerExceptionResolver, jwtService, userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .build();

        // Clear security context between tests
        SecurityContextHolder.clearContext();
    }

    // ── No auth header ───────────────────────────────────────────────────

    @Test
    void shouldSkipFilterWhenNoAuthHeader() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository, never()).findByUsername(any());
    }

    // ── Non-Bearer header ────────────────────────────────────────────────

    @Test
    void shouldSkipFilterWhenNotBearer() throws Exception {
        request.addHeader("Authorization", "Basic somecreds");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
    }

    // ── Valid token ──────────────────────────────────────────────────────

    @Test
    void shouldAuthenticateUserWithValidToken() throws Exception {
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(sampleUser, auth.getPrincipal());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }

    // ── Invalid token ────────────────────────────────────────────────────

    @Test
    void shouldHandleExceptionOnInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new RuntimeException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(handlerExceptionResolver).resolveException(same(request), same(response), isNull(),
                any(Exception.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ── User not found ───────────────────────────────────────────────────

    @Test
    void shouldHandleExceptionWhenUserNotFound() throws Exception {
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(handlerExceptionResolver).resolveException(same(request), same(response), isNull(),
                any(Exception.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ── Already authenticated ────────────────────────────────────────────

    @Test
    void shouldSkipWhenAlreadyAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities()));

        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("testuser");

        filter.doFilterInternal(request, response, filterChain);

        // Authentication should still be the original one
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        verify(userRepository, never()).findByUsername(any());
    }

    // ── Filter chain is always called ────────────────────────────────────

    @Test
    void shouldAlwaysCallFilterChain() throws Exception {
        // No auth header
        filter.doFilterInternal(request, response, filterChain);
        assertNotNull(filterChain.getRequest());

        // With valid token
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        filter.doFilterInternal(request, response, filterChain);
        assertNotNull(filterChain.getRequest());
    }
}
