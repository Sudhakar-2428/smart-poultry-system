package com.poultry.backend.dto;

import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.HealthStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenStatusPatchRequest {

    @NotNull(message = "Status is required")
    private ChickenStatus status;

    private HealthStatus healthStatus;

    private String remarks;
}
