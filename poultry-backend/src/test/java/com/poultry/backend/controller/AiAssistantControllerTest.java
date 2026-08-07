package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.AiAssistantDTOs;
import com.poultry.backend.service.AiAssistantService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAssistantService aiAssistantService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testProcessQuery_Success() throws Exception {
        AiAssistantDTOs.AiQueryResponse mockResponse = AiAssistantDTOs.AiQueryResponse.builder()
                .question("What is today's egg production?")
                .answer("Today's total egg production is 12 eggs.")
                .category("PRODUCTION")
                .relatedLinks(List.of("/egg-tracking.html"))
                .build();

        when(aiAssistantService.processQuery(any())).thenReturn(mockResponse);

        AiAssistantDTOs.AiQueryRequest request = new AiAssistantDTOs.AiQueryRequest("What is today's egg production?");

        mockMvc.perform(post("/api/v1/ai/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("PRODUCTION"))
                .andExpect(jsonPath("$.data.answer").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetRecommendations_Success() throws Exception {
        AiAssistantDTOs.AiRecommendationDTO rec = AiAssistantDTOs.AiRecommendationDTO.builder()
                .title("Deworming Vaccination Due")
                .description("Booster vaccination required")
                .category("VACCINATION")
                .priority("HIGH")
                .actionLink("/health-records.html")
                .build();

        when(aiAssistantService.getRecommendations()).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/ai/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Deworming Vaccination Due"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetSuggestedQuestions_Success() throws Exception {
        when(aiAssistantService.getSuggestedQuestions()).thenReturn(List.of("What is today's egg production?"));

        mockMvc.perform(get("/api/v1/ai/suggested-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("What is today's egg production?"));
    }
}
