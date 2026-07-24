package com.poultry.backend.dto;

import com.poultry.backend.entity.FarmRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    @NotNull(message = "New role is required")
    private FarmRole role;
}
