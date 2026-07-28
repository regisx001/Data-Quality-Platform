package com.regisx001.dQul.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String username;
    private String email;
    private String password;
    private String fullName;

    @Builder.Default
    private String role = "USER";

    public void setRole(String role) {
        this.role = role != null ? role : "USER";
    }
}
