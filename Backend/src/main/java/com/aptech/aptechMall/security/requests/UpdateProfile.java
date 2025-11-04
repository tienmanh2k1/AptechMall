package com.aptech.aptechMall.security.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfile {
    private String username;
    private String fullName;
    private String phone;
    private MultipartFile avatar;
    private String avatarUrl;
}

