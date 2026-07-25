package com.poultry.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinFarmTempRequest {

    @NotBlank(message = "Farm ID or Farm Unique ID is required")
    private String farmId;

    @NotBlank(message = "Worker ID or Email is required")
    private String workerId;

    @NotBlank(message = "Temporary Password is required")
    private String temporaryPassword;

    private String newPassword;
}
