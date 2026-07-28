package com.regisx001.dQul.authentication;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regisx001.dQul.authentication.AuthenticationRequest;
import com.regisx001.dQul.authentication.AuthenticationResponse;
import com.regisx001.dQul.authentication.RegisterRequest;
import com.regisx001.dQul.common.User;
import com.regisx001.dQul.common.UserRepository;
import com.regisx001.dQul.authentication.AuthenticationService;
import com.regisx001.dQul.security.JwtService;
import com.regisx001.dQul.common.UserService;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthenticationServiceImpl(UserService userService, JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                encodedPassword,
                request.getFullName(),
                request.getRole());

        String token = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .expiresIn(jwtService.getJwtExpiration())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String login = request.getLogin();

        // Determine whether the login is a username or email
        User user = login.contains("@")
                ? userService.getUserByEmail(login)
                : userService.getUserByUsername(login);

        if (!user.isActive()) {
            throw new IllegalStateException("User account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Record login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .expiresIn(jwtService.getJwtExpiration())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public boolean verifyToken(String token) {
        try {
            return !jwtService.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
