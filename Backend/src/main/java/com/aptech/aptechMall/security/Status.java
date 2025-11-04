package com.aptech.aptechMall.security;

public enum Status {
    ACTIVE, SUSPENDED, DELETED;
    public static Status fromString(String value) {
        return Status.valueOf(value.trim().toUpperCase());
    }
}
