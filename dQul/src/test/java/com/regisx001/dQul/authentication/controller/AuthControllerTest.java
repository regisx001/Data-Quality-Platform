package com.regisx001.dQul.authentication.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.authentication.dto.AuthenticationRequest;
import com.regisx001.dQul.authentication.dto.AuthenticationResponse;
import com.regisx001.dQul.authentication.dto.RegisterRequest;
import com.regisx001.dQul.authentication.exception.InvalidCredentialsException;
import com.regisx001.dQul.authentication.exception.UserDeactivatedException;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.exception.GlobalExceptionHandler;
import com.regisx001.dQul.common.service.LogsProducer;
import com.regisx001.dQul.authentication.service.AuthenticationService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        private MockMvc mockMvc;

        @Mock
        private AuthenticationService authenticationService;

        @Mock
        private LogsProducer logsProducer;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private static final String BASE_URL = "/api/v1/auth";

        private UUID userId;
        private User mockUser;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                mockUser = User.builder()
                                .id(userId)
                                .username("current")
                                .email("current@example.com")
                                .fullName("Current User")
                                .role("USER")
                                .build();

                AuthController controller = new AuthController(authenticationService, logsProducer, objectMapper);

                // Custom resolver to return mockUser for @AuthenticationPrincipal
                HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
                        @Override
                        public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(
                                                org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                        }

                        @Override
                        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return mockUser;
                        }
                };

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setCustomArgumentResolvers(authPrincipalResolver)
                                .build();
        }

        // ── register ───────────────────────────────────────────────────────

        @Test
        void register_shouldReturn201() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("newuser")
                                .email("new@example.com")
                                .password("password123")
                                .fullName("New User")
                                .role("USER")
                                .build();

                AuthenticationResponse response = AuthenticationResponse.builder()
                                .token("jwt-token")
                                .expiresIn(86400000L)
                                .userId(UUID.randomUUID())
                                .username("newuser")
                                .email("new@example.com")
                                .fullName("New User")
                                .role("USER")
                                .build();

                when(authenticationService.register(any(RegisterRequest.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL + "/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").value("jwt-token"))
                                .andExpect(jsonPath("$.username").value("newuser"))
                                .andExpect(jsonPath("$.email").value("new@example.com"));
        }

        @Test
        void register_shouldReturn400OnDuplicate() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("existing")
                                .email("e@example.com")
                                .password("pass123")
                                .fullName("Existing")
                                .build();

                when(authenticationService.register(any(RegisterRequest.class)))
                                .thenThrow(new IllegalArgumentException("Username 'existing' is already taken"));

                mockMvc.perform(post(BASE_URL + "/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Username 'existing' is already taken"));
        }

        // ── login ────────────────────────────────────────────────────────────

        @Test
        void login_shouldReturn200() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .login("testuser")
                                .password("pass123")
                                .build();

                AuthenticationResponse response = AuthenticationResponse.builder()
                                .token("jwt-token")
                                .expiresIn(86400000L)
                                .userId(UUID.randomUUID())
                                .username("testuser")
                                .email("test@example.com")
                                .fullName("Test User")
                                .role("USER")
                                .build();

                when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        void login_shouldReturn401OnInvalidCredentials() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .login("testuser")
                                .password("wrong")
                                .build();

                when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                                .thenThrow(new InvalidCredentialsException());

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void login_shouldReturn403OnDeactivatedUser() throws Exception {
                AuthenticationRequest request = AuthenticationRequest.builder()
                                .login("inactive")
                                .password("pass")
                                .build();

                when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                                .thenThrow(new UserDeactivatedException());

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));
        }

        // ── verify ──────────────────────────────────────────────────────────

        @Test
        void verify_shouldReturn200WhenValid() throws Exception {
                when(authenticationService.verifyToken("valid-token")).thenReturn(true);

                mockMvc.perform(post(BASE_URL + "/verify")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("valid-token"))
                                .andExpect(status().isOk());
        }

        @Test
        void verify_shouldReturn401WhenExpired() throws Exception {
                when(authenticationService.verifyToken("expired-token")).thenReturn(false);

                mockMvc.perform(post(BASE_URL + "/verify")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("expired-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
        }

        // ── me ──────────────────────────────────────────────────────────────

        @Test
        void me_shouldReturnCurrentUser() throws Exception {
                mockMvc.perform(get(BASE_URL + "/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(userId.toString()))
                                .andExpect(jsonPath("$.username").value("current"))
                                .andExpect(jsonPath("$.email").value("current@example.com"))
                                .andExpect(jsonPath("$.fullName").value("Current User"))
                                .andExpect(jsonPath("$.role").value("USER"));
        }
}
