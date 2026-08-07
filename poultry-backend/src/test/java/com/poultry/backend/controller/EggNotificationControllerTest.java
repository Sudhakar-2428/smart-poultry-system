package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.EggNotificationDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import com.poultry.backend.repository.EggCollectionNotificationRepository;
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
class EggNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private EggCollectionNotificationRepository eggCollectionNotificationRepository;

    @Autowired
    private ChickenTimelineRepository chickenTimelineRepository;

    private Chicken testHen;
    private EggCollectionNotification notification;

    @BeforeEach
    void setUp() {
        testHen = chickenRepository.save(Chicken.builder()
                .chickenCode("HEN-NOTIF-001")
                .name("Notification Hen")
                .gender(Gender.FEMALE)
                .category(ChickenCategory.LAYER)
                .breed(Breed.COUNTRY_CHICKEN)
                .origin(ChickenOrigin.FARM_BORN)
                .dateOfBirth(LocalDate.now().minusDays(200))
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        notification = eggCollectionNotificationRepository.save(EggCollectionNotification.builder()
                .chickenId(testHen.getId())
                .henCode(testHen.getChickenCode())
                .henName(testHen.getName())
                .breed("COUNTRY_CHICKEN")
                .notificationDate(LocalDate.now())
                .status("PENDING")
                .build());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetActivePendingNotifications() throws Exception {
        mockMvc.perform(get("/egg-notifications/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testConfirmEggCollection_YES() throws Exception {
        EggNotificationDTOs.ConfirmEggCollectionRequest req = EggNotificationDTOs.ConfirmEggCollectionRequest.builder()
                .healthyEggs(5)
                .brokenEggs(1)
                .damagedEggs(0)
                .remarks("Good size eggs")
                .build();

        mockMvc.perform(post("/egg-notifications/{id}/confirm", notification.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.healthyEggs", is(5)));

        EggCollectionNotification updated = eggCollectionNotificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testRecordNoEgg_NO() throws Exception {
        EggNotificationDTOs.NoEggReasonRequest req = EggNotificationDTOs.NoEggReasonRequest.builder()
                .reason("Brooding")
                .remarks("Hen sitting in nest box")
                .build();

        mockMvc.perform(post("/egg-notifications/{id}/no-egg", notification.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("NO_EGG")))
                .andExpect(jsonPath("$.data.noEggReason", is("Brooding")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testRescheduleNotification_STILL_NOT_NOW() throws Exception {
        EggNotificationDTOs.RescheduleNotificationRequest req = EggNotificationDTOs.RescheduleNotificationRequest.builder()
                .durationMinutes(60)
                .build();

        mockMvc.perform(post("/egg-notifications/{id}/reschedule", notification.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rescheduledUntil", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testTrigger06PMEscalation() throws Exception {
        mockMvc.perform(post("/egg-notifications/trigger-06pm-escalation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        EggCollectionNotification updated = eggCollectionNotificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals("ESCALATED", updated.getStatus());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetNotificationReport() throws Exception {
        mockMvc.perform(get("/egg-notifications/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalNotifications", greaterThanOrEqualTo(1)));
    }
}
