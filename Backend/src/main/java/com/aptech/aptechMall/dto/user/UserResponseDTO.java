package com.aptech.aptechMall.dto.user;

import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean emailVerified;
    private String phone;
    private Role role;
    private Status status;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
}
