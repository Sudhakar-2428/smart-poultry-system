package com.poultry.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiAssistantDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiQueryRequest {
        @NotBlank(message = "Prompt question is required")
        private String prompt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiQueryResponse {
        private String question;
        private String answer;
        private String category;
        private List<String> relatedLinks;
        private List<AiRecommendationDTO> recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecommendationDTO {
        private String title;
        private String description;
        private String category; // VACCINATION, PAIRING, FEED, HEALTH, EGG_COLLECTION
        private String priority; // HIGH, MEDIUM, LOW
        private String actionLink;
    }
}
