package com.poultry.backend.dto;

import com.poultry.backend.entity.IncubatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncubatorResponse {
    private Long id;
    private String batchCode;
    private Long eggBatchId;
    private String eggBatchCode;
    private Long sourceHenId;
    private String sourceHenCode;
    private String sourceHenName;
    private String sourceHenBreed;
    private Long maleChickenId;
    private String maleChickenCode;
    private String maleChickenName;
    private Long breedingPairId;
    private String pairingCode;

    private com.poultry.backend.entity.IncubationMethod incubationMethod;
    private String incubatorNumber;
    private String trayNumber;
    private String turningSchedule;
    private Long broodyHenId;
    private String broodyHenCode;
    private String broodyHenName;
    private String nestLocation;

    private Integer totalEggs;
    private Integer currentDay;
    private Double progressPercentage;

    private LocalDate startDate;
    private LocalDate expectedHatchDate;
    private LocalDate actualHatchDate;
    private IncubatorStatus status;
    private Double temperature;
    private Double humidity;
    private String notes;
    private Long remainingIncubationDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
