package com.poultry.backend.dto;

import com.poultry.backend.entity.EggBatchStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EggBatchStatusRequest {

    @NotNull(message = "Status is required")
    private EggBatchStatus status;

    private LocalDate actualHatchDate;
}
