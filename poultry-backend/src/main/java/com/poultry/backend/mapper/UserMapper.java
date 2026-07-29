package com.poultry.backend.mapper;

import com.poultry.backend.dto.RegisterRequest;
import com.poultry.backend.dto.UserDto;
import com.poultry.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /**
     * Map User entity to UserDto.
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        String farmRoleStr = user.getRole() != null ? user.getRole().name() : null;

        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .currentFarmRole(farmRoleStr)
                .isActive(user.isActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Map RegisterRequest DTO to User entity.
     */
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword())
                .role(request.getRole())
                .build();
    }
}
