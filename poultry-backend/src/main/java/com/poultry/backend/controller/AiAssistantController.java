package com.poultry.backend.controller;

import com.poultry.backend.dto.AiAssistantDTOs;
import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<AiAssistantDTOs.AiQueryResponse>> processQuery(@Valid @RequestBody AiAssistantDTOs.AiQueryRequest request) {
        log.info("REST request to process AI query: {}", request.getPrompt());
        AiAssistantDTOs.AiQueryResponse response = aiAssistantService.processQuery(request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI Query processed successfully"));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<AiAssistantDTOs.AiRecommendationDTO>>> getRecommendations() {
        log.info("REST request to fetch AI smart recommendations");
        List<AiAssistantDTOs.AiRecommendationDTO> recommendations = aiAssistantService.getRecommendations();
        return ResponseEntity.ok(ApiResponse.success(recommendations, "AI Recommendations retrieved successfully"));
    }

    @GetMapping("/suggested-questions")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestedQuestions() {
        log.info("REST request to fetch suggested AI questions");
        List<String> questions = aiAssistantService.getSuggestedQuestions();
        return ResponseEntity.ok(ApiResponse.success(questions, "Suggested questions retrieved successfully"));
    }
}
