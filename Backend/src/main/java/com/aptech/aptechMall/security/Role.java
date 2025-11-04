package com.aptech.aptechMall.security;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ADMIN,
    STAFF,
    CUSTOMER,
    ;

    public static Role fromString(String value) {
        return Role.valueOf(value.trim().toUpperCase());
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
