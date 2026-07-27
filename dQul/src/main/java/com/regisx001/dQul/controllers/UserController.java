package com.regisx001.dQul.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.dQul.domain.dto.responses.ApiErrorResponse;
import com.regisx001.dQul.domain.entities.User;
import com.regisx001.dQul.services.UserService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            User user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        try {
            User updated = userService.updateUser(id,
                    request.email(), request.fullName(), request.role());
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable UUID id) {
        try {
            User user = userService.activateUser(id);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID id) {
        try {
            User user = userService.deactivateUser(id);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Inner DTO ────────────────────────────────────────────────────────

    public record UpdateUserRequest(String email, String fullName, String role) {
    }
}
