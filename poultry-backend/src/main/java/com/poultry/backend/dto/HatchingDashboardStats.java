package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HatchingDashboardStats {
    private long activeHatchBatches;
    private long eggsUnderIncubation;
    private long expectedHatchToday;
    private long successfullyHatched;
    private long failedEggs;
    private double hatchSuccessRate;
}
