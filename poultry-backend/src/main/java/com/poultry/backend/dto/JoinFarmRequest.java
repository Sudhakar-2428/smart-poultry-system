package com.poultry.backend.dto;

import com.poultry.backend.entity.FarmRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinFarmRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Farm Unique ID is required")
    private String farmUniqueId;

    @NotBlank(message = "Farm Join Code is required")
    private String joinCode;

    @NotNull(message = "Farm role level is required")
    private FarmRole role;
}
