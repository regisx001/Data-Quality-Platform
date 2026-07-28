package com.regisx001.dQul.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.regisx001.dQul.common.User;
import com.regisx001.dQul.common.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountVerificationFilterTest {

    @Mock
    private UserRepository userRepository;

    private AccountVerificationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        filter = new AccountVerificationFilter(userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        UUID id = UUID.randomUUID();
        verifiedUser = User.builder()
                .id(id)
                .username("verified")
                .email("v@example.com")
                .passwordHash("encoded")
                .fullName("Verified")
                .role("USER")
                .active(true)
                .verified(true)
                .build();

        unverifiedUser = User.builder()
                .id(id)
                .username("unverified")
                .email("u@example.com")
                .passwordHash("encoded")
                .fullName("Unverified")
                .role("USER")
                .active(true)
                .verified(false)
                .build();

        SecurityContextHolder.clearContext();
    }

    // ── Excluded paths ───────────────────────────────────────────────────

    @Test
    void shouldSkipExcludedPaths() throws Exception {
        request.setRequestURI("/api/v1/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void shouldSkipRegisterPath() throws Exception {
        request.setRequestURI("/api/v1/auth/register");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
    }

    // ── Not authenticated ────────────────────────────────────────────────

    @Test
    void shouldSkipWhenNotAuthenticated() throws Exception {
        request.setRequestURI("/api/v1/users");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        verify(userRepository, never()).findByUsername(any());
    }

    // ── Authenticated + verified ─────────────────────────────────────────

    @Test
    void shouldAllowVerifiedUser() throws Exception {
        request.setRequestURI("/api/v1/users");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(verifiedUser, null, verifiedUser.getAuthorities()));

        when(userRepository.findByUsername("verified")).thenReturn(Optional.of(verifiedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus()); // no error written
    }

    // ── Authenticated + unverified ───────────────────────────────────────

    @Test
    void shouldBlockUnverifiedUser() throws Exception {
        request.setRequestURI("/api/v1/users");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(unverifiedUser, null, unverifiedUser.getAuthorities()));

        when(userRepository.findByUsername("unverified")).thenReturn(Optional.of(unverifiedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Account verification required"));
    }

    // ── Authenticated but user not in DB ─────────────────────────────────

    @Test
    void shouldAllowWhenUserNotFoundInDb() throws Exception {
        request.setRequestURI("/api/v1/users");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null, java.util.List.of()));

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    // ── Anonymous user ───────────────────────────────────────────────────

    @Test
    void shouldSkipForAnonymousUser() throws Exception {
        request.setRequestURI("/api/v1/users");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.List.of()));

        // Even though the name matches, the filter explicitly checks
        // !authentication.getName().equals("anonymousUser")
        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        verify(userRepository, never()).findByUsername(any());
    }
}
