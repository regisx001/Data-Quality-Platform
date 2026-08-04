package com.regisx001.dQul.common.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.exception.UserAlreadyExistsException;
import com.regisx001.dQul.common.exception.UserNotFoundException;
import com.regisx001.dQul.common.repository.UserRepository;
import com.regisx001.dQul.common.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public User createUser(String username, String email, String encodedPassword,
            String fullName, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "Email '" + email + "' is already in use");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(encodedPassword)
                .fullName(fullName)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(UUID id, String email, String fullName, String role) {
        User user = getUserById(id);

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new UserAlreadyExistsException(
                        "Email '" + email + "' is already in use");
            }
            user.setEmail(email);
        }
        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (role != null) {
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("id", id);
        }
        userRepository.deleteById(id);
    }

    // ── Account management ───────────────────────────────────────────────

    @Override
    public User deactivateUser(UUID id) {
        User user = getUserById(id);
        user.setActive(false);
        return userRepository.save(user);
    }

    @Override
    public User activateUser(UUID id) {
        User user = getUserById(id);
        user.setActive(true);
        return userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
