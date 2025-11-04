package com.aptech.aptechMall.security.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean emailVerified;
    private String phone;
    private LocalDateTime registeredAt;
    private LocalDateTime lastLogin;
}
