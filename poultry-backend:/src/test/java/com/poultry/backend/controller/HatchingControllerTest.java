package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.HatchResultRequest;
import com.poultry.backend.dto.IncubatorRequest;
import com.poultry.backend.dto.IncubatorStatusRequest;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private IncubatorBatchRepository incubatorBatchRepository;

    @Autowired
    private HatchResultRepository hatchResultRepository;

    @Autowired
    private BrooderBatchRepository brooderBatchRepository;

    @Autowired
    private FarmSettingRepository farmSettingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Chicken sourceHen;
    private EggBatch hatchingEggs;
    private EggBatch saleEggs;

    @BeforeEach
    void setUp() {
        brooderBatchRepository.deleteAll();
        hatchResultRepository.deleteAll();
        incubatorBatchRepository.deleteAll();
        eggBatchRepository.deleteAll();
        chickenRepository.deleteAll();
        farmSettingRepository.deleteAll();

        // Create standard laying hen
        sourceHen = Chicken.builder()
                .chickenCode("HEN-999")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(400))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(sourceHen);

        // Egg batch for hatching (100 total eggs, 0 damaged, purpose HATCHING)
        hatchingEggs = EggBatch.builder()
                .batchCode("EGG-HATCH")
                .batchDate(LocalDate.now().minusDays(5))
                .sourceHen(sourceHen)
                .totalEggs(100)
                .damagedEggs(0)
                .goodEggs(100)
                .status(EggBatchStatus.CREATED)
                .purpose(EggPurpose.HATCHING)
                .expectedHatchDate(LocalDate.now().plusDays(16))
                .build();
        eggBatchRepository.save(hatchingEggs);

        // Egg batch for sale (100 total eggs, purpose SALE)
        saleEggs = EggBatch.builder()
                .batchCode("EGG-SALE")
                .batchDate(LocalDate.now().minusDays(5))
                .sourceHen(sourceHen)
                .totalEggs(100)
                .damagedEggs(0)
                .goodEggs(100)
                .status(EggBatchStatus.CREATED)
                .purpose(EggPurpose.SALE)
                .expectedHatchDate(LocalDate.now().plusDays(16))
                .build();
        eggBatchRepository.save(saleEggs);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testIncubationCreation_Success() throws Exception {
        IncubatorRequest request = IncubatorRequest.builder()
                .batchCode("INC-001")
                .eggBatchId(hatchingEggs.getId())
                .startDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .temperature(37.5)
                .humidity(60.0)
                .build();

        mockMvc.perform(post("/incubators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchCode", is("INC-001")))
                .andExpect(jsonPath("$.data.eggBatchCode", is("EGG-HATCH")))
                .andExpect(jsonPath("$.data.expectedHatchDate", is(hatchingEggs.getExpectedHatchDate().toString())));

        // Verify underlying Egg Batch is now INCUBATING
        EggBatch updatedEggBatch = eggBatchRepository.findById(hatchingEggs.getId()).orElseThrow();
        assertEquals(EggBatchStatus.INCUBATING, updatedEggBatch.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testIncubationCreation_InvalidEggBatchPurpose_Rejected() throws Exception {
        IncubatorRequest request = IncubatorRequest.builder()
                .batchCode("INC-002")
                .eggBatchId(saleEggs.getId()) // purpose = SALE
                .startDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/incubators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("purpose = HATCHING")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testIncubationCreation_InvalidEggBatchStatus_Rejected() throws Exception {
        // Change hatching eggs status to finished HATCHED status
        hatchingEggs.setStatus(EggBatchStatus.HATCHED);
        eggBatchRepository.save(hatchingEggs);

        IncubatorRequest request = IncubatorRequest.builder()
                .batchCode("INC-003")
                .eggBatchId(hatchingEggs.getId())
                .startDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/incubators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("CREATED or BROODING")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testHatchResult_CountValidations() throws Exception {
        IncubatorBatch incubator = IncubatorBatch.builder()
                .batchCode("INC-VALIDATE")
                .eggBatch(hatchingEggs)
                .startDate(LocalDate.now().minusDays(21))
                .expectedHatchDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .build();
        incubatorBatchRepository.save(incubator);

        // Case 1: fertileEggs (101) > totalEggs (100)
        HatchResultRequest req1 = HatchResultRequest.builder()
                .incubatorBatchId(incubator.getId())
                .fertileEggs(101)
                .hatchedChicks(90)
                .deadEmbryos(10)
                .recordedDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/hatch-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fertile eggs greater than total eggs")));

        // Case 2: hatchedChicks (95) > fertileEggs (90)
        HatchResultRequest req2 = HatchResultRequest.builder()
                .incubatorBatchId(incubator.getId())
                .fertileEggs(90)
                .hatchedChicks(95)
                .deadEmbryos(5)
                .recordedDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/hatch-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("hatch more chicks than fertile eggs")));

        // Case 3: deadEmbryos (95) > fertileEggs (90)
        HatchResultRequest req3 = HatchResultRequest.builder()
                .incubatorBatchId(incubator.getId())
                .fertileEggs(90)
                .hatchedChicks(50)
                .deadEmbryos(95)
                .recordedDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/hatch-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("dead embryos greater than fertile eggs")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testHatchResult_Success_AutomaticChicksAndBrooder() throws Exception {
        IncubatorBatch incubator = IncubatorBatch.builder()
                .batchCode("INC-SUCCESS")
                .eggBatch(hatchingEggs)
                .startDate(LocalDate.now().minusDays(21))
                .expectedHatchDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .build();
        incubatorBatchRepository.save(incubator);

        // Configure custom brooder settings period to 15 days in DB
        FarmSetting periodSetting = new FarmSetting("BROODER_PERIOD", "15");
        farmSettingRepository.save(periodSetting);

        HatchResultRequest request = HatchResultRequest.builder()
                .incubatorBatchId(incubator.getId())
                .fertileEggs(90)
                .hatchedChicks(80)
                .deadEmbryos(5)
                .recordedDate(LocalDate.now())
                .remarks("Healthy hatch session")
                .build();

        mockMvc.perform(post("/hatch-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEggs", is(100)))
                .andExpect(jsonPath("$.data.fertileEggs", is(90)))
                .andExpect(jsonPath("$.data.hatchedChicks", is(80)))
                .andExpect(jsonPath("$.data.unhatchedEggs", is(20))) // 100 - 80 = 20
                .andExpect(jsonPath("$.data.hatchPercentage", closeTo(80.0, 0.001)));

        // 1. Verify Incubator is marked COMPLETED
        IncubatorBatch completedInc = incubatorBatchRepository.findById(incubator.getId()).orElseThrow();
        assertEquals(IncubatorStatus.COMPLETED, completedInc.getStatus());
        assertEquals(LocalDate.now(), completedInc.getActualHatchDate());

        // 2. Verify underlying Egg Batch is marked HATCHED
        EggBatch completedEgg = eggBatchRepository.findById(hatchingEggs.getId()).orElseThrow();
        assertEquals(EggBatchStatus.HATCHED, completedEgg.getStatus());

        // 3. Verify Chicks automatically created (80 chick records expected)
        List<Chicken> chicks = chickenRepository.findAll().stream()
                .filter(c -> c.getCategory() == ChickenCategory.CHICK)
                .toList();
        assertEquals(80, chicks.size());

        Chicken sampleChick = chicks.get(0);
        assertEquals(ChickenStatus.BROODER, sampleChick.getStatus());
        assertEquals(Gender.UNKNOWN, sampleChick.getGender());
        assertEquals(LocalDate.now(), sampleChick.getDateOfBirth());
        assertEquals(sourceHen.getBreed(), sampleChick.getBreed()); // Inherited Hen breed
        assertEquals(sourceHen.getId(), sampleChick.getMotherId()); // motherId references Hen

        // 4. Verify Brooder automatically created (expectedEndDate = StartDate + 15 days)
        List<BrooderBatch> brooders = brooderBatchRepository.findAll();
        assertEquals(1, brooders.size());
        BrooderBatch activeBrooder = brooders.get(0);
        assertEquals(BrooderStatus.ACTIVE, activeBrooder.getStatus());
        assertEquals(LocalDate.now(), activeBrooder.getStartDate());
        assertEquals(LocalDate.now().plusDays(15), activeBrooder.getExpectedEndDate());
        assertEquals("BRD-INC-SUCCESS", activeBrooder.getBrooderCode());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testIncubatorStatus_Update() throws Exception {
        IncubatorBatch incubator = IncubatorBatch.builder()
                .batchCode("INC-STATUS")
                .eggBatch(hatchingEggs)
                .startDate(LocalDate.now())
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .status(IncubatorStatus.ACTIVE)
                .build();
        incubatorBatchRepository.save(incubator);

        IncubatorStatusRequest patchReq = new IncubatorStatusRequest(IncubatorStatus.CANCELLED, null);

        mockMvc.perform(patch("/incubators/" + incubator.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testSearchAndPagination_WorkerAccess() throws Exception {
        IncubatorBatch inc = IncubatorBatch.builder()
                .batchCode("WORKER-INC-CHECK")
                .eggBatch(hatchingEggs)
                .startDate(LocalDate.now())
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .status(IncubatorStatus.ACTIVE)
                .build();
        incubatorBatchRepository.save(inc);

        // WORKER can query and search incubator batches
        mockMvc.perform(get("/incubators")
                        .param("batchCode", "WORKER-INC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testWriterAuthorization_WorkerForbidden() throws Exception {
        IncubatorRequest request = IncubatorRequest.builder()
                .batchCode("INC-FORBDN")
                .eggBatchId(hatchingEggs.getId())
                .startDate(LocalDate.now())
                .status(IncubatorStatus.ACTIVE)
                .build();

        // WORKER tries to schedule incubator -> Forbidden
        mockMvc.perform(post("/incubators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticated_Blocked() throws Exception {
        mockMvc.perform(get("/incubators"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/hatch-results"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/brooders"))
                .andExpect(status().isUnauthorized());
    }
}
