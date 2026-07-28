package com.regisx001.dQul.common.service;

import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.common.domain.User;

public interface UserService {

    // ── CRUD ──────────────────────────────────────────────────────────────

    User createUser(String username, String email, String encodedPassword,
            String fullName, String role);

    User getUserById(UUID id);

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(UUID id, String email, String fullName, String role);

    void deleteUser(UUID id);

    // ── Account management ───────────────────────────────────────────────

    User deactivateUser(UUID id);

    User activateUser(UUID id);

    // ── Helpers ───────────────────────────────────────────────────────────

    boolean isUsernameTaken(String username);

    boolean isEmailTaken(String email);
}
