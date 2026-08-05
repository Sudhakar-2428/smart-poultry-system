package com.poultry.backend.dto;

import com.poultry.backend.entity.PairStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HenPairingProfileResponse {
    private Long pairId;
    private String pairCode;
    
    private Long roosterId;
    private String roosterCode;
    private String roosterName;
    private String roosterBreed;
    private String roosterPhotoUrl;

    private LocalDate pairingDate;
    private Long daysSincePairing;
    private String currentStage; // Waiting, Ready for Egg Laying, Transferred (Egg Collection), Archived
    private PairStatus status;
}
