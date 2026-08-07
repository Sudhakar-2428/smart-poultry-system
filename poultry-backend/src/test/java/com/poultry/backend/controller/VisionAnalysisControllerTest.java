package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.VisionAnalysisDTOs;
import com.poultry.backend.service.VisionAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class VisionAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisionAnalysisService visionAnalysisService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAnalyzeImage_Success() throws Exception {
        VisionAnalysisDTOs.VisionAnalysisResponse mockResponse = VisionAnalysisDTOs.VisionAnalysisResponse.builder()
                .chickenId(1L)
                .chickenCode("HEN-101")
                .detectedCondition("Healthy - Normal Visual Assessment")
                .confidenceScore(97.8)
                .isolationRequired(false)
                .vetConsultationRecommended(false)
                .symptoms(List.of(VisionAnalysisDTOs.SymptomDetail.builder().feature("Comb").status("Bright Red").severity("LOW").build()))
                .treatmentGuidance("No treatment required")
                .recommendations(List.of("Routine monitoring"))
                .healthRecordId(10L)
                .timelineEventId(20L)
                .build();

        when(visionAnalysisService.analyzeImage(any(), anyString())).thenReturn(mockResponse);

        VisionAnalysisDTOs.VisionAnalysisRequest request = VisionAnalysisDTOs.VisionAnalysisRequest.builder()
                .chickenId(1L)
                .imageUrl("https://example.com/photo.jpg")
                .notes("Normal bird visual check")
                .build();

        mockMvc.perform(post("/api/v1/health/vision-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chickenCode").value("HEN-101"))
                .andExpect(jsonPath("$.data.confidenceScore").value(97.8));
    }
}
