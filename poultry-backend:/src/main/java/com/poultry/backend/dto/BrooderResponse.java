package com.poultry.backend.dto;

import com.poultry.backend.entity.BrooderStatus;
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
public class BrooderResponse {
    private Long id;
    private String brooderCode;
    private Long hatchResultId;
    private String incubatorBatchCode;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private BrooderStatus status;
    private String location;
    private String remarks;
    private Long remainingBrooderDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
