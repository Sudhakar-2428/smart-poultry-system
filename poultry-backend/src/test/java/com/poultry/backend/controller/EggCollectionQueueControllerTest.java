package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.EggCollectionQueueDTOs;
import com.poultry.backend.service.EggCollectionQueueService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EggCollectionQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EggCollectionQueueService eggCollectionQueueService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetTodayQueue_Success() throws Exception {
        EggCollectionQueueDTOs.EggQueueItemResponse item = EggCollectionQueueDTOs.EggQueueItemResponse.builder()
                .id(1L)
                .henCode("HEN-101")
                .henName("Rosy")
                .status("PENDING")
                .build();

        EggCollectionQueueDTOs.EggQueueSummaryResponse summary = EggCollectionQueueDTOs.EggQueueSummaryResponse.builder()
                .queueDate(LocalDate.now())
                .totalHens(1L)
                .pendingCount(1L)
                .completedCount(0L)
                .rescheduledCount(0L)
                .escalatedCount(0L)
                .items(List.of(item))
                .build();

        when(eggCollectionQueueService.getTodayQueue(anyString())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/egg-queue/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.items[0].henCode").value("HEN-101"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testConfirmQueueItem_Success() throws Exception {
        EggCollectionQueueDTOs.EggQueueItemResponse item = EggCollectionQueueDTOs.EggQueueItemResponse.builder()
                .id(1L)
                .status("COMPLETED")
                .healthyEggs(1)
                .build();

        when(eggCollectionQueueService.confirmQueueItem(any(), any(), anyString())).thenReturn(item);

        EggCollectionQueueDTOs.ConfirmQueueItemRequest request = new EggCollectionQueueDTOs.ConfirmQueueItemRequest(1, 0, 0, "Good egg");

        mockMvc.perform(post("/api/v1/egg-queue/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
