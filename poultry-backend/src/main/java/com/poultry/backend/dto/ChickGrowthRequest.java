package com.poultry.backend.dto;

import com.poultry.backend.entity.GrowthStage;
import com.poultry.backend.entity.HealthStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickGrowthRequest {

    @NotNull(message = "Chicken ID is required")
    private Long chickenId;

    @NotNull(message = "Growth date is required")
    private LocalDate growthDate;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than zero")
    private Double weight;

    private Double height;

    @NotNull(message = "Health status is required")
    private HealthStatus healthStatus;

    @NotNull(message = "Growth stage is required")
    private GrowthStage growthStage;

    private String remarks;
}
