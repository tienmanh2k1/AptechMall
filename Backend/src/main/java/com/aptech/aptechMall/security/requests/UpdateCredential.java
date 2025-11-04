package com.aptech.aptechMall.security.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCredential {
    private String oldEmail;
    private String email;
    private String oldPassword;
    private String password;
}
