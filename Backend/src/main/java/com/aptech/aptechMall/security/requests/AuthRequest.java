package com.aptech.aptechMall.security.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest { //DTO is shared by both authenticate() and authenticateGoogle(), do not insert @NotBlank in any fields
    private String username;
    private String password;
    private String fullname;
    private String email;
    private String googleSub;
}
