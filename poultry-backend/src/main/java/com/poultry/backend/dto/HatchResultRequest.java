package com.poultry.backend.dto;

import jakarta.validation.constraints.Min;
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
public class HatchResultRequest {

    @NotNull(message = "Incubator batch ID is required")
    private Long incubatorBatchId;

    @NotNull(message = "Fertile eggs is required")
    @Min(value = 0, message = "Fertile eggs cannot be negative")
    private Integer fertileEggs;

    @NotNull(message = "Hatched chicks is required")
    @Min(value = 0, message = "Hatched chicks cannot be negative")
    private Integer hatchedChicks;

    @Min(value = 0, message = "Healthy chicks cannot be negative")
    private Integer healthyChicks;

    @Min(value = 0, message = "Weak chicks cannot be negative")
    private Integer weakChicks;

    @Min(value = 0, message = "Dead chicks cannot be negative")
    private Integer deadChicks;

    @NotNull(message = "Dead embryos is required")
    @Min(value = 0, message = "Dead embryos cannot be negative")
    private Integer deadEmbryos;

    @NotNull(message = "Recorded date is required")
    private LocalDate recordedDate;

    private String remarks;
}
