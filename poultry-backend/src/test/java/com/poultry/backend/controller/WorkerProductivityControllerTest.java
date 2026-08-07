package com.poultry.backend.controller;

import com.poultry.backend.dto.WorkerProductivityDTOs;
import com.poultry.backend.service.WorkerProductivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class WorkerProductivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerProductivityService workerProductivityService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetTodayProductivitySummary_Success() throws Exception {
        WorkerProductivityDTOs.WorkerProductivitySummary summary = WorkerProductivityDTOs.WorkerProductivitySummary.builder()
                .date(LocalDate.now())
                .totalScheduledHens(10L)
                .completedHens(6L)
                .pendingHens(4L)
                .overallCompletionRatePercentage(60.0)
                .bestPerformingWorker("WORKER1")
                .workerLeaderboard(List.of())
                .liveActivityFeed(List.of())
                .build();

        when(workerProductivityService.getTodayProductivitySummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/worker-productivity/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallCompletionRatePercentage").value(60.0))
                .andExpect(jsonPath("$.data.bestPerformingWorker").value("WORKER1"));
    }
}
