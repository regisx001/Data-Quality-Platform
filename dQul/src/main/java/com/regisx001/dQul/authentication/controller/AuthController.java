package com.regisx001.dQul.authentication.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.authentication.dto.AuthenticationRequest;
import com.regisx001.dQul.authentication.dto.AuthenticationResponse;
import com.regisx001.dQul.authentication.dto.RegisterRequest;
import com.regisx001.dQul.authentication.exception.InvalidCredentialsException;
import com.regisx001.dQul.authentication.exception.InvalidTokenException;
import com.regisx001.dQul.authentication.exception.UserDeactivatedException;
import com.regisx001.dQul.authentication.service.AuthenticationService;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.dto.LogEvent;
import com.regisx001.dQul.common.exception.BaseAppException;
import com.regisx001.dQul.common.service.LogsProducer;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")

public class AuthController {

    private static final String SERVICE_NAME = "dQul-api";
    private static final String CATEGORY = "AUTH";

    private final AuthenticationService authenticationService;
    private final LogsProducer logsProducer;
    private final ObjectMapper objectMapper;

    public AuthController(AuthenticationService authenticationService, LogsProducer logsProducer,
            ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.logsProducer = logsProducer;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse response;
        try {
            response = authenticationService.register(request);
        } catch (RuntimeException e) {
            emitAuthLog("WARN", "Registration failed", "/api/v1/auth/register",
                    statusOf(e), "REGISTRATION_FAILED", metadata("reason", reasonOf(e)));
            throw e;
        }
        emitAuthLog("INFO", "Registration succeeded", "/api/v1/auth/register",
                HttpStatus.CREATED.value(), "REGISTRATION_SUCCEEDED",
                metadata("userId", userId(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        String loginMethod = loginMethod(request.getLogin());
        AuthenticationResponse response;
        try {
            response = authenticationService.authenticate(request);
        } catch (UserDeactivatedException e) {
            emitAuthLog("WARN", "Login blocked", "/api/v1/auth/login",
                    HttpStatus.FORBIDDEN.value(), "LOGIN_BLOCKED",
                    metadata("reason", "ACCOUNT_INACTIVE", "loginMethod", loginMethod));
            throw e;
        } catch (InvalidCredentialsException e) {
            emitAuthLog("WARN", "Login failed", "/api/v1/auth/login",
                    HttpStatus.UNAUTHORIZED.value(), "LOGIN_FAILED",
                    metadata("reason", "INVALID_CREDENTIALS", "loginMethod", loginMethod));
            throw e;
        }
        emitAuthLog("INFO", "Login succeeded", "/api/v1/auth/login",
                HttpStatus.OK.value(), "LOGIN_SUCCEEDED",
                metadata("userId", userId(response), "loginMethod", loginMethod));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyToken(@RequestBody String token) {
        boolean valid = authenticationService.verifyToken(token);
        if (valid) {
            return ResponseEntity.ok().build();
        }
        emitAuthLog("WARN", "Token verification failed", "/api/v1/auth/verify",
                HttpStatus.UNAUTHORIZED.value(), "TOKEN_VERIFICATION_FAILED",
                metadata("reason", "INVALID_OR_EXPIRED"));
        throw new InvalidTokenException();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticationResponse> currentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new InvalidTokenException("Not authenticated");
        }

        AuthenticationResponse response = AuthenticationResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Emits a log event for an auth action via the {@link LogsProducer}. The event
     * flows to
     * {@code platform-logs-topic} and is consumed/persisted by the
     * {@code dQul-logs} microservice.
     * Production is asynchronous; failures are logged by the producer, never thrown
     * here.
     */
    private void emitAuthLog(String level, String message, String path, int statusCode,
            String event, String metadataJson) {
        LogEvent eventLog = LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .logLevel(level)
                .category(CATEGORY)
                .message(message)
                .path(path)
                .httpMethod("POST")
                .statusCode(statusCode)
                .metadata(metadataJson)
                .build();
        logsProducer.produce(eventLog);
    }

    private String loginMethod(String login) {
        return login != null && login.contains("@") ? "EMAIL" : "USERNAME";
    }

    private String userId(AuthenticationResponse response) {
        return response.getUserId() != null ? String.valueOf(response.getUserId()) : null;
    }

    private int statusOf(RuntimeException e) {
        return e instanceof BaseAppException ? ((BaseAppException) e).getStatus().value()
                : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String reasonOf(RuntimeException e) {
        return e instanceof BaseAppException ? ((BaseAppException) e).getErrorCode() : "INTERNAL_ERROR";
    }

    private String metadata(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                map.put(String.valueOf(keyValues[i]), value);
            }
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
