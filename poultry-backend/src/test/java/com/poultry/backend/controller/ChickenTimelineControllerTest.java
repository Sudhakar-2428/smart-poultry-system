package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.ChickenTimelineDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChickenTimelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ChickenTimelineRepository chickenTimelineRepository;

    private Chicken testHen;

    @BeforeEach
    void setUp() {
        testHen = chickenRepository.save(Chicken.builder()
                .chickenCode("HEN-TL-001")
                .name("Timeline Hen")
                .gender(Gender.FEMALE)
                .category(ChickenCategory.LAYER)
                .breed(Breed.COUNTRY_CHICKEN)
                .origin(ChickenOrigin.FARM_BORN)
                .dateOfBirth(LocalDate.now().minusDays(100))
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                .chicken(testHen)
                .eventType("REGISTERED")
                .title("Registered")
                .description("Registered into farm inventory")
                .moduleName("REGISTRATION")
                .createdBy("System")
                .build());

        chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                .chicken(testHen)
                .eventType("VACCINATION")
                .title("Deworming Vaccination")
                .description("Administered 1ml Dewormer vaccine")
                .moduleName("HEALTH")
                .createdBy("Dr. Smith")
                .build());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetChickenTimeline_Success() throws Exception {
        mockMvc.perform(get("/chickens/{chickenId}/timeline", testHen.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].moduleNavigationLink", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetChickenTimeline_WithFilter() throws Exception {
        mockMvc.perform(get("/chickens/{chickenId}/timeline", testHen.getId())
                        .param("eventType", "VACCINATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].eventType", is("VACCINATION")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAddManualNote_Success() throws Exception {
        ChickenTimelineDTOs.CreateTimelineNoteRequest req = ChickenTimelineDTOs.CreateTimelineNoteRequest.builder()
                .title("Routine Observation")
                .description("Hen is active and laying normally.")
                .moduleName("OBSERVATION")
                .build();

        mockMvc.perform(post("/chickens/{chickenId}/timeline/notes", testHen.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Routine Observation")));

        assertEquals(3, chickenTimelineRepository.findByChickenIdOrderByTimestampDesc(testHen.getId()).size());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetTimelineReport() throws Exception {
        mockMvc.perform(get("/chickens/timeline/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEvents", greaterThanOrEqualTo(2)));
    }
}
