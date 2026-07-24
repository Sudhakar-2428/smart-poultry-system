package com.poultry.backend.dto;

import com.poultry.backend.entity.IncubatorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncubatorStatusRequest {
    @NotNull(message = "Status is required")
    private IncubatorStatus status;

    private LocalDate actualHatchDate;
}
