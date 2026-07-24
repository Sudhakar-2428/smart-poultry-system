package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.AdultTransitionRequest;
import com.poultry.backend.dto.ChickGrowthRequest;
import com.poultry.backend.dto.GenderUpdateRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChickGrowthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ChickGrowthRecordRepository growthRecordRepository;

    @Autowired
    private FarmSettingRepository farmSettingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Chicken testChick;
    private Chicken testAdult;

    @BeforeEach
    void setUp() {
        growthRecordRepository.deleteAll();
        chickenRepository.deleteAll();
        farmSettingRepository.deleteAll();

        // Create standard pre-adult chick (Category = CHICK, Status = GROWING)
        testChick = Chicken.builder()
                .chickenCode("CK-GROWING-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.CHICK)
                .gender(Gender.UNKNOWN)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.GROWING)
                .weight(0.5)
                .build();
        testChick = chickenRepository.save(testChick);

        // Create adult bird (Category = LAYER, Status = ACTIVE)
        testAdult = Chicken.builder()
                .chickenCode("HEN-ADULT-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(180))
                .status(ChickenStatus.ACTIVE)
                .weight(1.8)
                .build();
        testAdult = chickenRepository.save(testAdult);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGrowthRecordCreation_Success() throws Exception {
        ChickGrowthRequest request = ChickGrowthRequest.builder()
                .chickenId(testChick.getId())
                .growthDate(LocalDate.now())
                .weight(0.75)
                .height(15.2)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.GROWER)
                .remarks("Healthy development")
                .build();

        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.weight", is(0.75)))
                .andExpect(jsonPath("$.data.height", is(15.2)))
                .andExpect(jsonPath("$.data.chickenId", is(testChick.getId().intValue())))
                .andExpect(jsonPath("$.data.ageInDays", is(30)))
                .andExpect(jsonPath("$.data.currentAge", is(30)))
                .andExpect(jsonPath("$.data.daysUntilAdultTransition", is(120))); // 150 - 30

        // Weight validation synced to Chicken entity
        Chicken updatedChicken = chickenRepository.findById(testChick.getId()).orElseThrow();
        assertEquals(0.75, updatedChicken.getWeight());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateDailyRecord_Rejected() throws Exception {
        ChickGrowthRequest request = ChickGrowthRequest.builder()
                .chickenId(testChick.getId())
                .growthDate(LocalDate.now())
                .weight(0.75)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.GROWER)
                .build();

        // First creation -> returns 201 Created
        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation on same day -> returns 409 Conflict
        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only one growth record is allowed per chick per day")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInvalidWeightAndFutureDate_Rejected() throws Exception {
        // Invalid Weight <= 0
        ChickGrowthRequest invWeightReq = ChickGrowthRequest.builder()
                .chickenId(testChick.getId())
                .growthDate(LocalDate.now())
                .weight(0.0)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.GROWER)
                .build();

        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invWeightReq)))
                .andExpect(status().isBadRequest());

        // Future Date
        ChickGrowthRequest futureDateReq = ChickGrowthRequest.builder()
                .chickenId(testChick.getId())
                .growthDate(LocalDate.now().plusDays(1))
                .weight(0.8)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.GROWER)
                .build();

        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Growth dates cannot be in the future")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenderUpdate_Success() throws Exception {
        GenderUpdateRequest request = new GenderUpdateRequest(Gender.MALE);

        mockMvc.perform(patch("/chickens/" + testChick.getId() + "/gender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.gender", is("MALE")));

        Chicken updated = chickenRepository.findById(testChick.getId()).orElseThrow();
        assertEquals(Gender.MALE, updated.getGender());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testInvalidGenderChangeForAdult_ByManager_Forbidden() throws Exception {
        // testAdult has status ACTIVE, and is category LAYER. Manager cannot update gender.
        GenderUpdateRequest request = new GenderUpdateRequest(Gender.MALE);

        mockMvc.perform(patch("/chickens/" + testAdult.getId() + "/gender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Gender cannot be changed after the bird reaches ADULT status")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenderChangeForAdult_ByAdmin_Allowed() throws Exception {
        GenderUpdateRequest request = new GenderUpdateRequest(Gender.MALE);

        mockMvc.perform(patch("/chickens/" + testAdult.getId() + "/gender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender", is("MALE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdultTransition_Success() throws Exception {
        AdultTransitionRequest request = new AdultTransitionRequest(ChickenCategory.LAYER, "Transitioning healthy laying hen");

        mockMvc.perform(patch("/chickens/" + testChick.getId() + "/adult-transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.category", is("LAYER")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        Chicken transitioned = chickenRepository.findById(testChick.getId()).orElseThrow();
        assertEquals(ChickenCategory.LAYER, transitioned.getCategory());
        assertEquals(ChickenStatus.ACTIVE, transitioned.getStatus());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testAuthorization_WorkerBlocked() throws Exception {
        ChickGrowthRequest request = ChickGrowthRequest.builder()
                .chickenId(testChick.getId())
                .growthDate(LocalDate.now())
                .weight(0.75)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.GROWER)
                .build();

        // Worker cannot create growth record
        mockMvc.perform(post("/chick-growth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Worker cannot update gender
        GenderUpdateRequest genReq = new GenderUpdateRequest(Gender.FEMALE);
        mockMvc.perform(patch("/chickens/" + testChick.getId() + "/gender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isForbidden());

        // Worker cannot trigger adult transition
        AdultTransitionRequest transReq = new AdultTransitionRequest(ChickenCategory.LAYER, "Transition");
        mockMvc.perform(patch("/chickens/" + testChick.getId() + "/adult-transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testSearchAndPagination_WorkerAllowed() throws Exception {
        // Save direct growth records to query
        ChickGrowthRecord record1 = ChickGrowthRecord.builder()
                .chicken(testChick)
                .growthDate(LocalDate.now().minusDays(5))
                .weight(0.55)
                .height(12.0)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.STARTER)
                .gender(Gender.UNKNOWN)
                .ageInDays(25)
                .build();
        growthRecordRepository.save(record1);

        mockMvc.perform(get("/chick-growth")
                        .param("growthStage", "STARTER")
                        .param("minWeight", "0.5")
                        .param("maxWeight", "0.6")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].growthStage", is("STARTER")));
    }
}
