package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoosterPairingProfileResponse {
    private Long roosterId;
    private String roosterCode;
    private String roosterName;

    private Integer totalPairings;
    private Integer activePairings;
    private Integer completedPairings;
    private Integer totalFertileEggs;
    private Integer totalChicksProduced;

    private List<LinkedHenSummary> linkedHens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedHenSummary {
        private Long henId;
        private String henCode;
        private String henName;
        private String breed;
        private String photoUrl;
        private String pairingStatus;
        private String pairingDate;
    }
}
