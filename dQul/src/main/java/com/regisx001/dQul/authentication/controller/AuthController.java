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
import com.regisx001.dQul.common.responses.ApiErrorResponse;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.authentication.service.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthenticationResponse response = authenticationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody String token) {
        boolean valid = authenticationService.verifyToken(token);
        if (valid) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Token is invalid or expired")
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Not authenticated")
                            .build());
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
}
