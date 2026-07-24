package com.poultry.backend.dto;

import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggPurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggBatchRequest {

    @NotBlank(message = "Batch code is required")
    @Size(max = 50, message = "Batch code cannot exceed 50 characters")
    private String batchCode;

    @NotNull(message = "Batch date is required")
    private LocalDate batchDate;

    private Long sourceHenId;

    @NotNull(message = "Total eggs is required")
    @Min(value = 1, message = "Total eggs must be greater than zero")
    private Integer totalEggs;

    @NotNull(message = "Damaged eggs is required")
    @Min(value = 0, message = "Damaged eggs count cannot be negative")
    private Integer damagedEggs;

    @NotNull(message = "Status is required")
    private EggBatchStatus status;

    @NotNull(message = "Purpose is required")
    private EggPurpose purpose;

    private LocalDate actualHatchDate;

    private String remarks;
}
