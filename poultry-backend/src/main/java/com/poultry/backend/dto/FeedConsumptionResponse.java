package com.poultry.backend.dto;

import com.poultry.backend.entity.FeedingType;
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
public class FeedConsumptionResponse {
    private Long id;
    private Long feedItemId;
    private String feedName;
    private Long chickenId;
    private String chickenCode;
    private Long brooderBatchId;
    private String brooderCode;
    private LocalDate consumptionDate;
    private Double quantity;
    private FeedingType feedingType;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
