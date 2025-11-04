package com.aptech.aptechMall.model.enums;

public enum AddressType {
    HOME, OFFICE, OTHER;
    public static AddressType fromString(String value) {
        return AddressType.valueOf(value.trim().toUpperCase());
    }
}
