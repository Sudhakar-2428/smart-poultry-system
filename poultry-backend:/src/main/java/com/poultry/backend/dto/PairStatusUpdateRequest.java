package com.poultry.backend.dto;

import com.poultry.backend.entity.PairStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private PairStatus status;

    private LocalDate endDate;
}
