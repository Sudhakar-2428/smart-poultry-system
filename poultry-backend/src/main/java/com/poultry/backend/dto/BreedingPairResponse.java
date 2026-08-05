package com.poultry.backend.dto;

import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
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
public class BreedingPairResponse {
    private Long id;
    private String pairCode;
    private Long maleChickenId;
    private String maleChickenCode;
    private String maleChickenName;
    private String maleChickenBreed;
    private String maleChickenPhotoUrl;
    private Long femaleChickenId;
    private String femaleChickenCode;
    private String femaleChickenName;
    private String femaleChickenBreed;
    private String femaleChickenPhotoUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private PairStatus status;
    private BreedingPurpose breedingPurpose;
    private com.poultry.backend.entity.PairingType pairingType;
    private Integer expectedEggProduction;
    private String remarks;

    // Automatic date calculations
    private Long daysSincePairing;
    private LocalDate expectedEggLayingDate;
    private LocalDateTime eggLayingStartedAt;
    private LocalDateTime archivedAt;
    private LocalDate archiveDate;
    private String elapsedDuration;

    // Aggregated Performance Metrics
    private Integer eggsProducedCount;
    private Integer hatchBatchesCount;
    private Integer chicksBornCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
