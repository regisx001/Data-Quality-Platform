package com.regisx001.dQul.authentication.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.dQul.authentication.dto.AuthenticationRequest;
import com.regisx001.dQul.authentication.dto.AuthenticationResponse;
import com.regisx001.dQul.authentication.dto.RegisterRequest;
import com.regisx001.dQul.authentication.exception.InvalidTokenException;
import com.regisx001.dQul.authentication.service.AuthenticationService;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.dto.LogEvent;
import com.regisx001.dQul.common.service.LogsProducer;

@RestController
@RequestMapping("/api/v1/auth")

public class AuthController {

    private static final String SERVICE_NAME = "dQul-api";

    private final AuthenticationService authenticationService;
    private final LogsProducer logsProducer;

    public AuthController(AuthenticationService authenticationService, LogsProducer logsProducer) {
        this.authenticationService = authenticationService;
        this.logsProducer = logsProducer;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        emitAuthLog("INFO", "User registered: " + response.getUsername(),
                "/api/v1/auth/register", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        emitAuthLog("INFO", "User logged in: " + response.getUsername(),
                "/api/v1/auth/login", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyToken(@RequestBody String token) {
        boolean valid = authenticationService.verifyToken(token);
        if (valid) {
            emitAuthLog("DEBUG", "Token verified",
                    "/api/v1/auth/verify", null);
            return ResponseEntity.ok().build();
        }
        emitAuthLog("WARN", "Token verification failed",
                "/api/v1/auth/verify", null);
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

        emitAuthLog("INFO", "Current user profile fetched: " + response.getUsername(),
                "/api/v1/auth/me", response);

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
    private void emitAuthLog(String level, String message, String path, AuthenticationResponse response) {
        LogEvent.LogEventBuilder builder = LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .logLevel(level)
                .category("AUTH")
                .message(message)
                .path(path)
                .httpMethod("POST");
        if (response != null && response.getUserId() != null) {
            builder.userId(String.valueOf(response.getUserId()));
            builder.userEmail(response.getEmail());
        }
        logsProducer.produce(builder.build());
    }
}
