package com.poultry.backend.dto;

import com.poultry.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private String systemRole;
    private String currentFarmRole;
    private boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getSystemRole() {
        return systemRole != null ? systemRole : (role != null ? role.name() : "USER");
    }
}
