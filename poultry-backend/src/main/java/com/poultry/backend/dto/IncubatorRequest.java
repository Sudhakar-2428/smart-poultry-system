package com.poultry.backend.dto;

import com.poultry.backend.entity.IncubatorStatus;
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
public class IncubatorRequest {

    @NotBlank(message = "Batch code is required")
    @Size(max = 50, message = "Batch code cannot exceed 50 characters")
    private String batchCode;

    @NotNull(message = "Egg batch ID is required")
    private Long eggBatchId;

    private Long sourceHenId;
    private Long maleChickenId;
    private Long breedingPairId;

    private com.poultry.backend.entity.IncubationMethod incubationMethod;

    private String incubatorNumber;
    private String trayNumber;
    private String turningSchedule;

    private Long broodyHenId;
    private String nestLocation;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Status is required")
    private IncubatorStatus status;

    private Double temperature;

    private Double humidity;

    private String notes;
}
