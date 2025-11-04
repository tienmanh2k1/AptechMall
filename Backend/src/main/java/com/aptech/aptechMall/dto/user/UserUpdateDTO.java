package com.aptech.aptechMall.dto.user;

import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Size(max = 191)
    private String fullName;

    @Size(max = 512)
    private String avatarUrl;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 30)
    private String email;

    private Role role;
    private Status status;
}
