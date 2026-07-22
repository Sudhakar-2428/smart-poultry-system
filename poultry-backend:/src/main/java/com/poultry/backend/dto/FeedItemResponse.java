package com.poultry.backend.dto;

import com.poultry.backend.entity.FeedStatus;
import com.poultry.backend.entity.FeedType;
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
public class FeedItemResponse {
    private Long id;
    private String feedCode;
    private String feedName;
    private FeedType feedType;
    private String description;
    private String unit;
    private Double minimumStock;
    private Double currentStock;
    private Double unitCost;
    private String storageLocation;
    private LocalDate expiryDate;
    private FeedStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
