package com.regisx001.dQul.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.exception.UserAlreadyExistsException;
import com.regisx001.dQul.common.exception.UserNotFoundException;
import com.regisx001.dQul.common.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private User sampleUser;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
        sampleId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPass123")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createUser ───────────────────────────────────────────────────────

    @Test
    void createUser_shouldSaveAndReturnUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User created = userService.createUser("newuser", "new@example.com",
                "encodedPass", "New User", "USER");

        assertNotNull(created.getId());
        assertEquals("newuser", created.getUsername());
        assertEquals("new@example.com", created.getEmail());
        assertEquals("encodedPass", created.getPasswordHash());
        assertEquals("New User", created.getFullName());
        assertEquals("USER", created.getRole());
        assertTrue(created.isActive());
        assertNotNull(created.getCreatedAt());

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameTaken() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.createUser("existing", "e@example.com",
                        "pass", "Existing", "USER"));

        assertTrue(ex.getMessage().contains("already taken"));
        verify(userRepository).existsByUsername("existing");
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_shouldThrowWhenEmailTaken() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.createUser("newuser", "taken@example.com",
                        "pass", "New", "USER"));

        assertTrue(ex.getMessage().contains("already in use"));
        verify(userRepository).existsByEmail("taken@example.com");
        verify(userRepository, never()).save(any());
    }

    // ── getUserById ──────────────────────────────────────────────────────

    @Test
    void getUserById_shouldReturnUserWhenFound() {
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));

        User found = userService.getUserById(sampleId);

        assertEquals(sampleUser, found);
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(UUID.randomUUID()));
    }

    // ── getUserByUsername ────────────────────────────────────────────────

    @Test
    void getUserByUsername_shouldReturnUserWhenFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        User found = userService.getUserByUsername("testuser");

        assertEquals(sampleUser, found);
    }

    @Test
    void getUserByUsername_shouldThrowWhenNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserByUsername("unknown"));
    }

    // ── getUserByEmail ───────────────────────────────────────────────────

    @Test
    void getUserByEmail_shouldReturnUserWhenFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        User found = userService.getUserByEmail("test@example.com");

        assertEquals(sampleUser, found);
    }

    @Test
    void getUserByEmail_shouldThrowWhenNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserByEmail("unknown@example.com"));
    }

    // ── getAllUsers ──────────────────────────────────────────────────────

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertTrue(users.contains(sampleUser));
    }

    // ── updateUser ───────────────────────────────────────────────────────

    @Test
    void updateUser_shouldUpdateAllFields() {
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(sampleId, "new@example.com", "New Name", "ADMIN");

        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New Name", updated.getFullName());
        assertEquals("ADMIN", updated.getRole());
    }

    @Test
    void updateUser_shouldSkipNullFields() {
        String originalEmail = sampleUser.getEmail();
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(sampleId, null, null, null);

        assertEquals(originalEmail, updated.getEmail());
        assertEquals("Test User", updated.getFullName());
        assertEquals("USER", updated.getRole());
    }

    @Test
    void updateUser_shouldThrowOnDuplicateEmail() {
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.updateUser(sampleId, "dup@example.com", null, null));
    }

    // ── deleteUser ───────────────────────────────────────────────────────

    @Test
    void deleteUser_shouldDeleteWhenExists() {
        when(userRepository.existsById(sampleId)).thenReturn(true);

        userService.deleteUser(sampleId);

        verify(userRepository).deleteById(sampleId);
    }

    @Test
    void deleteUser_shouldThrowWhenNotFound() {
        when(userRepository.existsById(any(UUID.class))).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser(UUID.randomUUID()));
    }

    // ── deactivateUser / activateUser ────────────────────────────────────

    @Test
    void deactivateUser_shouldSetActiveFalse() {
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User deactivated = userService.deactivateUser(sampleId);

        assertFalse(deactivated.isActive());
    }

    @Test
    void activateUser_shouldSetActiveTrue() {
        sampleUser.setActive(false);
        when(userRepository.findById(sampleId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User activated = userService.activateUser(sampleId);

        assertTrue(activated.isActive());
    }

    // ── isUsernameTaken / isEmailTaken ───────────────────────────────────

    @Test
    void isUsernameTaken_shouldReturnTrueWhenExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertTrue(userService.isUsernameTaken("testuser"));
    }

    @Test
    void isUsernameTaken_shouldReturnFalseWhenNotExists() {
        when(userRepository.existsByUsername("unknown")).thenReturn(false);

        assertFalse(userService.isUsernameTaken("unknown"));
    }

    @Test
    void isEmailTaken_shouldReturnTrueWhenExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertTrue(userService.isEmailTaken("test@example.com"));
    }

    @Test
    void isEmailTaken_shouldReturnFalseWhenNotExists() {
        when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

        assertFalse(userService.isEmailTaken("unknown@example.com"));
    }
}
