package com.poultry.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    SUPER_ADMIN,
    USER;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
