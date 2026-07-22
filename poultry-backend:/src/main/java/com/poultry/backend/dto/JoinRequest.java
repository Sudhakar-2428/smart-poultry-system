package com.poultry.backend.dto;

import com.poultry.backend.entity.FarmRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequest {
    @NotBlank(message = "Join code is required")
    private String joinCode;

    @NotNull(message = "Requested role is required")
    private FarmRole role;
}
