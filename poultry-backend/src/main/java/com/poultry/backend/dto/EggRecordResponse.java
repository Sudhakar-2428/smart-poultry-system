package com.poultry.backend.dto;

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
public class EggRecordResponse {
    private Long id;
    private LocalDate recordDate;
    private Long henId;
    private String henCode;
    private Integer numberOfEggs;
    private Integer damagedEggs;
    private Integer goodEggs;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
