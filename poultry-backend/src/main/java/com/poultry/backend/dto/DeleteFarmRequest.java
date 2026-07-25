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
public class DeleteFarmRequest {

    @NotBlank(message = "Confirmation text is required")
    private String confirmationText;

    @NotBlank(message = "Current password is required")
    private String password;
}
