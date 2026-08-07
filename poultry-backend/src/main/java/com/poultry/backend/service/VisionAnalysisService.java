package com.poultry.backend.service;

import com.poultry.backend.dto.VisionAnalysisDTOs;

public interface VisionAnalysisService {
    VisionAnalysisDTOs.VisionAnalysisResponse analyzeImage(VisionAnalysisDTOs.VisionAnalysisRequest request, String currentUser);
}
