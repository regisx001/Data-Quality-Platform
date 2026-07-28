package com.regisx001.dQul.authentication;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String token;
    private long expiresIn;
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
}
