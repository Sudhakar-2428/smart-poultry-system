package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.EggBatchRequest;
import com.poultry.backend.dto.EggBatchStatusRequest;
import com.poultry.backend.dto.EggRecordRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.EggBatchRepository;
import com.poultry.backend.repository.EggRecordRepository;
import com.poultry.backend.repository.FarmSettingRepository;
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
class EggTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private EggRecordRepository eggRecordRepository;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private FarmSettingRepository farmSettingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Chicken activeHen;
    private Chicken rooster;

    @BeforeEach
    void setUp() {
        eggRecordRepository.deleteAll();
        eggBatchRepository.deleteAll();
        chickenRepository.deleteAll();
        farmSettingRepository.deleteAll();

        // Create standard laying hen
        activeHen = Chicken.builder()
                .chickenCode("HEN-1")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(300))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(activeHen);

        // Create standard rooster
        rooster = Chicken.builder()
                .chickenCode("RST-1")
                .breed(Breed.ROSS_308)
                .category(ChickenCategory.ROOSTER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(300))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(rooster);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDailyEggRecording_Success() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now())
                .henId(activeHen.getId())
                .numberOfEggs(10)
                .damagedEggs(2)
                .remarks("Good morning check")
                .build();

        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.numberOfEggs", is(10)))
                .andExpect(jsonPath("$.data.damagedEggs", is(2)))
                .andExpect(jsonPath("$.data.goodEggs", is(8)))
                .andExpect(jsonPath("$.data.henCode", is("HEN-1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDailyEggRecording_InvalidHen_NotFound() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now())
                .henId(9999L) // Non-existent hen
                .numberOfEggs(5)
                .damagedEggs(0)
                .build();

        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDailyEggRecording_Rooster_Rejected() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now())
                .henId(rooster.getId()) // Rooster source
                .numberOfEggs(5)
                .damagedEggs(0)
                .build();

        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Roosters cannot lay eggs")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDailyEggRecording_FutureDate_Rejected() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now().plusDays(1)) // Future date
                .henId(activeHen.getId())
                .numberOfEggs(5)
                .damagedEggs(0)
                .build();

        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDailyEggRecording_DamagedExceedsTotal_Rejected() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now())
                .henId(activeHen.getId())
                .numberOfEggs(5)
                .damagedEggs(6) // Damaged (6) > Total (5)
                .build();

        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Damaged eggs cannot exceed total eggs")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateEggBatch_DuplicateBatchCode() throws Exception {
        // Save initial batch
        EggBatch batch = EggBatch.builder()
                .batchCode("BAT-DUPLICATE")
                .batchDate(LocalDate.now())
                .totalEggs(100)
                .damagedEggs(5)
                .goodEggs(95)
                .status(EggBatchStatus.CREATED)
                .purpose(EggPurpose.HATCHING)
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .build();
        eggBatchRepository.save(batch);

        EggBatchRequest request = EggBatchRequest.builder()
                .batchCode("BAT-DUPLICATE") // Duplicate code
                .batchDate(LocalDate.now())
                .totalEggs(50)
                .damagedEggs(0)
                .status(EggBatchStatus.INCUBATING)
                .purpose(EggPurpose.HATCHING)
                .build();

        mockMvc.perform(post("/egg-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateEggBatch_ExpectedHatchDateSettingsLookup() throws Exception {
        // Seed incubation setting override to 25 days instead of default 21
        FarmSetting setting = new FarmSetting("INCUBATION_DAYS", "25");
        farmSettingRepository.save(setting);

        EggBatchRequest request = EggBatchRequest.builder()
                .batchCode("BAT-SETTINGS")
                .batchDate(LocalDate.now())
                .totalEggs(100)
                .damagedEggs(10)
                .status(EggBatchStatus.CREATED)
                .purpose(EggPurpose.HATCHING)
                .build();

        mockMvc.perform(post("/egg-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                // BatchDate + 25 days
                .andExpect(jsonPath("$.data.expectedHatchDate", is(LocalDate.now().plusDays(25).toString())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testBatchStatusUpdate_SuccessAndCalculations() throws Exception {
        EggBatch batch = EggBatch.builder()
                .batchCode("BAT-CALC")
                .batchDate(LocalDate.now().minusDays(10))
                .totalEggs(100)
                .damagedEggs(10)
                .goodEggs(90)
                .status(EggBatchStatus.INCUBATING)
                .purpose(EggPurpose.HATCHING)
                .expectedHatchDate(LocalDate.now().plusDays(11))
                .build();
        eggBatchRepository.save(batch);

        // Fetch detailed batch view to inspect calculations before status changes
        mockMvc.perform(get("/egg-batches/" + batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchAge", is(10)))
                .andExpect(jsonPath("$.data.daysRemaining", is(11)))
                .andExpect(jsonPath("$.data.hatchPercentage", nullValue())); // null since not hatched or failed yet

        // Patch status to HATCHED
        EggBatchStatusRequest statusRequest = new EggBatchStatusRequest(EggBatchStatus.HATCHED, LocalDate.now());

        mockMvc.perform(patch("/egg-batches/" + batch.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("HATCHED")))
                .andExpect(jsonPath("$.data.daysRemaining", is(0))) // 0 matches complete
                .andExpect(jsonPath("$.data.hatchPercentage", closeTo(90.0, 0.001))); // good/total * 100
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testSearchAndPagination_WorkerReadAccess() throws Exception {
        // Create a few daily egg records
        for (int i = 1; i <= 3; i++) {
            EggRecord record = EggRecord.builder()
                    .recordDate(LocalDate.now().minusDays(i))
                    .hen(activeHen)
                    .numberOfEggs(10)
                    .damagedEggs(1)
                    .build();
            eggRecordRepository.save(record);
        }

        // WORKER context can search egg records
        mockMvc.perform(get("/egg-records")
                        .param("henId", activeHen.getId().toString())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testWriterAuthorization_WorkersForbidden() throws Exception {
        EggRecordRequest request = EggRecordRequest.builder()
                .recordDate(LocalDate.now())
                .henId(activeHen.getId())
                .numberOfEggs(10)
                .damagedEggs(0)
                .build();

        // WORKER attempts write modification -> Forbidden
        mockMvc.perform(post("/egg-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticated_UnauthorizedAccess() throws Exception {
        // Unauthenticated calls receive 401 Unauthorized
        mockMvc.perform(get("/egg-records"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/egg-batches"))
                .andExpect(status().isUnauthorized());
    }
}
