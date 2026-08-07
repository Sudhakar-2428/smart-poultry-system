package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.ChickRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChickRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private IncubatorBatchRepository incubatorBatchRepository;

    @Autowired
    private HatchResultRepository hatchResultRepository;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private ChickenTimelineRepository chickenTimelineRepository;

    @Autowired
    private ChickRegistrationService chickRegistrationService;

    private Chicken farmHen;
    private Chicken purchasedHen;
    private Chicken rooster;
    private IncubatorBatch batch1;
    private IncubatorBatch batch2;

    @BeforeEach
    void setUp() {
        farmHen = chickenRepository.save(Chicken.builder()
                .chickenCode("101")
                .name("Hen 101")
                .gender(Gender.FEMALE)
                .category(ChickenCategory.LAYER)
                .breed(Breed.COUNTRY_CHICKEN)
                .origin(ChickenOrigin.FARM_BORN)
                .dateOfBirth(LocalDate.now().minusDays(200))
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        purchasedHen = chickenRepository.save(Chicken.builder()
                .chickenCode("PB01-005")
                .name("Purchased Hen")
                .gender(Gender.FEMALE)
                .category(ChickenCategory.LAYER)
                .breed(Breed.RHODE_ISLAND_RED)
                .origin(ChickenOrigin.PURCHASED)
                .dateOfBirth(LocalDate.now().minusDays(200))
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        rooster = chickenRepository.save(Chicken.builder()
                .chickenCode("ROOSTER-001")
                .name("Rooster King")
                .gender(Gender.MALE)
                .category(ChickenCategory.ROOSTER)
                .breed(Breed.COUNTRY_CHICKEN)
                .origin(ChickenOrigin.FARM_BORN)
                .dateOfBirth(LocalDate.now().minusDays(200))
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        EggBatch eggBatch1 = eggBatchRepository.save(EggBatch.builder()
                .batchCode("EB-101-01")
                .batchDate(LocalDate.now().minusDays(30))
                .sourceHen(farmHen)
                .maleChicken(rooster)
                .totalEggs(10)
                .status(EggBatchStatus.INCUBATING)
                .build());

        batch1 = incubatorBatchRepository.save(IncubatorBatch.builder()
                .batchCode("INC-CHICK-101")
                .eggBatch(eggBatch1)
                .sourceHen(farmHen)
                .maleChicken(rooster)
                .startDate(LocalDate.now().minusDays(21))
                .expectedHatchDate(LocalDate.now())
                .status(IncubatorStatus.COMPLETED)
                .build());

        HatchResult hr1 = hatchResultRepository.save(HatchResult.builder()
                .incubatorBatch(batch1)
                .totalEggs(10)
                .fertileEggs(10)
                .hatchedChicks(3)
                .healthyChicks(3)
                .weakChicks(0)
                .deadChicks(0)
                .deadEmbryos(0)
                .unhatchedEggs(0)
                .hatchPercentage(100.0)
                .recordedDate(LocalDate.now())
                .build());

        EggBatch eggBatch2 = eggBatchRepository.save(EggBatch.builder()
                .batchCode("EB-PB01-005-01")
                .batchDate(LocalDate.now().minusDays(30))
                .sourceHen(purchasedHen)
                .maleChicken(rooster)
                .totalEggs(5)
                .status(EggBatchStatus.INCUBATING)
                .build());

        batch2 = incubatorBatchRepository.save(IncubatorBatch.builder()
                .batchCode("INC-CHICK-PB01")
                .eggBatch(eggBatch2)
                .sourceHen(purchasedHen)
                .maleChicken(rooster)
                .startDate(LocalDate.now().minusDays(21))
                .expectedHatchDate(LocalDate.now())
                .status(IncubatorStatus.COMPLETED)
                .build());

        HatchResult hr2 = hatchResultRepository.save(HatchResult.builder()
                .incubatorBatch(batch2)
                .totalEggs(5)
                .fertileEggs(5)
                .hatchedChicks(2)
                .healthyChicks(2)
                .weakChicks(0)
                .deadChicks(0)
                .deadEmbryos(0)
                .unhatchedEggs(0)
                .hatchPercentage(100.0)
                .recordedDate(LocalDate.now())
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAutomaticChickRegistration_FarmBornHen() throws Exception {
        mockMvc.perform(post("/chick-registration/hatch-batch/{batchId}", batch1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalRegisteredChicks", is(3)))
                .andExpect(jsonPath("$.data.registeredChicks[0].chickenCode", is("101-1-001")))
                .andExpect(jsonPath("$.data.registeredChicks[1].chickenCode", is("101-1-002")))
                .andExpect(jsonPath("$.data.registeredChicks[2].chickenCode", is("101-1-003")));

        List<Chicken> chicks = chickenRepository.findAll().stream()
                .filter(c -> c.getCategory() == ChickenCategory.CHICK)
                .toList();

        assertEquals(3, chicks.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAutomaticChickRegistration_PurchasedHen() throws Exception {
        mockMvc.perform(post("/chick-registration/hatch-batch/{batchId}", batch2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalRegisteredChicks", is(2)))
                .andExpect(jsonPath("$.data.registeredChicks[0].chickenCode", is("PB01-005-1-001")))
                .andExpect(jsonPath("$.data.registeredChicks[1].chickenCode", is("PB01-005-1-002")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetParentChickStats() throws Exception {
        chickRegistrationService.registerChicksForHatchBatch(batch1.getId());

        mockMvc.perform(get("/chick-registration/parents/{chickenId}/stats", farmHen.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalChicksProduced", is(3)))
                .andExpect(jsonPath("$.data.totalHatchBatches", is(1)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetChickReports() throws Exception {
        chickRegistrationService.registerChicksForHatchBatch(batch1.getId());

        mockMvc.perform(get("/chick-registration/reports")
                        .param("reportType", "MOTHER")
                        .param("filterId", farmHen.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalChicks", is(3)));
    }
}
