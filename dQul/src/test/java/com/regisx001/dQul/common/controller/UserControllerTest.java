package com.regisx001.dQul.common.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.common.domain.User;
import com.regisx001.dQul.common.exception.GlobalExceptionHandler;
import com.regisx001.dQul.common.service.UserService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "/api/v1/users";

    private User sampleUser;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── getAllUsers ──────────────────────────────────────────────────────

    @Test
    void getAllUsers_shouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUser));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void getAllUsers_shouldReturnEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    // ── getUserById ──────────────────────────────────────────────────────

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        when(userService.getUserById(sampleId)).thenReturn(sampleUser);

        mockMvc.perform(get(BASE_URL + "/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserById_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userService.getUserById(unknownId))
                .thenThrow(new EntityNotFoundException("User not found with id: " + unknownId));

        mockMvc.perform(get(BASE_URL + "/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── getUserByUsername ────────────────────────────────────────────────

    @Test
    void getUserByUsername_shouldReturnUser() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(sampleUser);

        mockMvc.perform(get(BASE_URL + "/by-username/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserByUsername_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getUserByUsername("unknown"))
                .thenThrow(new EntityNotFoundException("User not found with username: unknown"));

        mockMvc.perform(get(BASE_URL + "/by-username/{username}", "unknown"))
                .andExpect(status().isNotFound());
    }

    // ── getUserByEmail ───────────────────────────────────────────────────

    @Test
    void getUserByEmail_shouldReturnUser() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(sampleUser);

        mockMvc.perform(get(BASE_URL + "/by-email/{email}", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserByEmail_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getUserByEmail("unknown@example.com"))
                .thenThrow(new EntityNotFoundException("User not found with email: unknown@example.com"));

        mockMvc.perform(get(BASE_URL + "/by-email/{email}", "unknown@example.com"))
                .andExpect(status().isNotFound());
    }

    // ── updateUser ───────────────────────────────────────────────────────

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        User updatedUser = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("updated@example.com")
                .passwordHash("encoded")
                .fullName("Updated Name")
                .role("ADMIN")
                .active(true)
                .createdAt(sampleUser.getCreatedAt())
                .build();

        when(userService.updateUser(eq(sampleId), eq("updated@example.com"),
                eq("Updated Name"), eq("ADMIN")))
                .thenReturn(updatedUser);

        String body = """
                {"email":"updated@example.com","fullName":"Updated Name","role":"ADMIN"}
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", sampleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUser_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userService.updateUser(eq(unknownId), any(), any(), any()))
                .thenThrow(new EntityNotFoundException("User not found"));

        String body = "{\"email\":\"a@b.com\"}";

        mockMvc.perform(put(BASE_URL + "/{id}", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_shouldReturn400OnDuplicateEmail() throws Exception {
        when(userService.updateUser(eq(sampleId), eq("dup@example.com"), any(), any()))
                .thenThrow(new IllegalArgumentException("Email 'dup@example.com' is already in use"));

        String body = "{\"email\":\"dup@example.com\"}";

        mockMvc.perform(put(BASE_URL + "/{id}", sampleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── deleteUser ───────────────────────────────────────────────────────

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        doNothing().when(userService).deleteUser(sampleId);

        mockMvc.perform(delete(BASE_URL + "/{id}", sampleId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("User not found"))
                .when(userService).deleteUser(unknownId);

        mockMvc.perform(delete(BASE_URL + "/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    // ── activateUser ─────────────────────────────────────────────────────

    @Test
    void activateUser_shouldReturnActivatedUser() throws Exception {
        User activated = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .createdAt(sampleUser.getCreatedAt())
                .build();

        when(userService.activateUser(sampleId)).thenReturn(activated);

        mockMvc.perform(patch(BASE_URL + "/{id}/activate", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activateUser_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userService.activateUser(unknownId))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(patch(BASE_URL + "/{id}/activate", unknownId))
                .andExpect(status().isNotFound());
    }

    // ── deactivateUser ───────────────────────────────────────────────────

    @Test
    void deactivateUser_shouldReturnDeactivatedUser() throws Exception {
        User deactivated = User.builder()
                .id(sampleId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded")
                .fullName("Test User")
                .role("USER")
                .active(false)
                .createdAt(sampleUser.getCreatedAt())
                .build();

        when(userService.deactivateUser(sampleId)).thenReturn(deactivated);

        mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateUser_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userService.deactivateUser(unknownId))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", unknownId))
                .andExpect(status().isNotFound());
    }
}
