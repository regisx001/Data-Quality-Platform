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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyToken(@RequestBody String token) {
        boolean valid = authenticationService.verifyToken(token);
        if (valid) {
            return ResponseEntity.ok().build();
        }
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
}
