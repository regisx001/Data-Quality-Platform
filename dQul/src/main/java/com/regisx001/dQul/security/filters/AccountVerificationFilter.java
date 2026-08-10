package com.regisx001.dQul.security.filters;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.common.responses.ApiErrorResponse;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Arrays;

// @Component
@RequiredArgsConstructor
public class AccountVerificationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<String> excludedPaths = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/verify",
            "/api/v1/auth/me",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/refresh-token");

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (excludedPaths.stream().anyMatch(requestURI::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            User user = userRepository.findByUsername(authentication.getName()).orElse(null);

            if (user != null && !user.isVerified()) {
                ApiErrorResponse error = ApiErrorResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .message("Account verification required. Please check your email for verification code.")
                        .build();

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

}
