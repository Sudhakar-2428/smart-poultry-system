package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.VisionAnalysisDTOs;
import com.poultry.backend.service.VisionAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class VisionAnalysisController {

    private final VisionAnalysisService visionAnalysisService;

    @PostMapping("/vision-analysis")
    public ResponseEntity<ApiResponse<VisionAnalysisDTOs.VisionAnalysisResponse>> analyzeImage(
            @Valid @RequestBody VisionAnalysisDTOs.VisionAnalysisRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request for AI Vision Analysis on Chicken ID: {} by user: {}", request.getChickenId(), currentUser);
        VisionAnalysisDTOs.VisionAnalysisResponse response = visionAnalysisService.analyzeImage(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "AI Computer Vision diagnostic analysis completed successfully"));
    }
}
