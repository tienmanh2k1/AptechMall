package com.aptech.aptechMall.security.requests;

import com.aptech.aptechMall.security.Role;
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
    private Role role;
    private LocalDateTime registeredAt;
    private LocalDateTime lastLogin;
}
