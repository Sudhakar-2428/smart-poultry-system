package com.poultry.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class VisionAnalysisDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisionAnalysisRequest {
        @NotNull(message = "Chicken ID is required")
        private Long chickenId;
        private String imageUrl;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisionAnalysisResponse {
        private Long chickenId;
        private String chickenCode;
        private String imageUrl;
        private String detectedCondition;
        private Double confidenceScore; // e.g. 94.5%
        private Boolean isolationRequired;
        private Boolean vetConsultationRecommended;
        private List<SymptomDetail> symptoms;
        private String treatmentGuidance;
        private List<String> recommendations;
        private Long healthRecordId;
        private Long timelineEventId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymptomDetail {
        private String feature; // Comb, Eye, Feather, Skin, Posture
        private String status;  // Normal, Pale, Swollen, Lesions, Drooping
        private String severity; // LOW, MEDIUM, HIGH
    }
}
