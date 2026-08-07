package com.poultry.backend.service;

import com.poultry.backend.dto.AiAssistantDTOs;

import java.util.List;

public interface AiAssistantService {
    AiAssistantDTOs.AiQueryResponse processQuery(AiAssistantDTOs.AiQueryRequest request);
    List<AiAssistantDTOs.AiRecommendationDTO> getRecommendations();
    List<String> getSuggestedQuestions();
}
