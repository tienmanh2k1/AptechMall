package com.aptech.aptechMall.security.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfile {
    private String username;
    private String fullName;
    private String phone;
    private String avatarUrl;
}

