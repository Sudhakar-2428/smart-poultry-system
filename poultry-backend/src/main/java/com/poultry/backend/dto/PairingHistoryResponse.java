package com.poultry.backend.dto;

import com.poultry.backend.entity.PairStatus;
import com.poultry.backend.entity.PairingType;
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
public class PairingHistoryResponse {
    private Long id;
    private String pairCode;
    
    // Hen
    private Long femaleChickenId;
    private String femaleChickenCode;
    private String femaleChickenName;
    private String femaleChickenBreed;
    private String femaleChickenPhotoUrl;

    // Rooster
    private Long maleChickenId;
    private String maleChickenCode;
    private String maleChickenName;
    private String maleChickenBreed;
    private String maleChickenPhotoUrl;

    private PairingType pairingType;
    private LocalDate pairingDate;
    private LocalDate eggLayingDate;
    private LocalDate archiveDate;
    private String duration;

    private Integer eggsProduced;
    private Integer hatchBatches;
    private Integer totalChicksBorn;

    private PairStatus currentStatus;
    private String remarks;
    private LocalDateTime createdAt;
}
