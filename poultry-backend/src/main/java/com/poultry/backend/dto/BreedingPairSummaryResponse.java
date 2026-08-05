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
public class BreedingPairSummaryResponse {
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
    private PairStatus status;
    private BreedingPurpose breedingPurpose;
    private com.poultry.backend.entity.PairingType pairingType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysSincePairing;
    private LocalDate expectedEggLayingDate;
    private LocalDateTime eggLayingStartedAt;
    private LocalDate archiveDate;
}
