package com.poultry.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return Role.USER.name();
        }
        return role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        return Role.fromString(dbData);
    }
}
