package com.poultry.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggRecordRequest {

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    private LocalDate recordDate;

    @NotNull(message = "Hen ID is required")
    private Long henId;

    @NotNull(message = "Number of eggs is required")
    @Min(value = 1, message = "Egg count must be greater than zero")
    private Integer numberOfEggs;

    @NotNull(message = "Damaged eggs is required")
    @Min(value = 0, message = "Damaged eggs count cannot be negative")
    private Integer damagedEggs;

    private String remarks;
}
