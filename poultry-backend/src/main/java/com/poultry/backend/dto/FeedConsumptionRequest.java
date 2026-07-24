package com.poultry.backend.dto;

import com.poultry.backend.entity.FeedingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedConsumptionRequest {

    @NotNull(message = "Feed item ID is required")
    private Long feedItemId;

    private Long chickenId;

    private Long brooderBatchId;

    @NotNull(message = "Consumption date is required")
    private LocalDate consumptionDate;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    @NotNull(message = "Feeding type is required")
    private FeedingType feedingType;

    private String remarks;
}
