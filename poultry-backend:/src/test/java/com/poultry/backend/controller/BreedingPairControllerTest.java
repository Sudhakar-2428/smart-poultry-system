package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.BreedingPairRequest;
import com.poultry.backend.dto.PairStatusUpdateRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.BreedingPairRepository;
import com.poultry.backend.repository.ChickenRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BreedingPairControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private BreedingPairRepository breedingPairRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Chicken validMale;
    private Chicken validFemale;
    private Chicken invalidMaleGender;
    private Chicken inactiveChicken;

    @BeforeEach
    void setUp() {
        breedingPairRepository.deleteAll();
        chickenRepository.deleteAll();

        // Standard valid male rooster (ACTIVE, MALE, ROOSTER)
        validMale = Chicken.builder()
                .chickenCode("ROO-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.ROOSTER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(300))
                .status(ChickenStatus.ACTIVE)
                .weight(2.5)
                .build();
        validMale = chickenRepository.save(validMale);

        // Standard valid female hen (ACTIVE, FEMALE, LAYER)
        validFemale = Chicken.builder()
                .chickenCode("HEN-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(290))
                .status(ChickenStatus.ACTIVE)
                .weight(2.0)
                .build();
        validFemale = chickenRepository.save(validFemale);

        // Female rooster (invalid category/gender matching - gender is female but code claims male)
        invalidMaleGender = Chicken.builder()
                .chickenCode("ROO-INV")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.ROOSTER)
                .gender(Gender.FEMALE) // invalid male gender
                .dateOfBirth(LocalDate.now().minusDays(280))
                .status(ChickenStatus.ACTIVE)
                .weight(2.2)
                .build();
        invalidMaleGender = chickenRepository.save(invalidMaleGender);

        // Inactive hen
        inactiveChicken = Chicken.builder()
                .chickenCode("HEN-INACTIVE")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(200))
                .status(ChickenStatus.SOLD) // inactive status
                .weight(1.9)
                .build();
        inactiveChicken = chickenRepository.save(inactiveChicken);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testValidPairCreation_Success() throws Exception {
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-01")
                .maleChickenId(validMale.getId())
                .femaleChickenId(validFemale.getId())
                .startDate(LocalDate.now().minusDays(5))
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.SELECTIVE_BREEDING)
                .expectedEggProduction(250)
                .remarks("Prime selection")
                .build();

        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pairCode", is("PAIR-01")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.maleChickenCode", is("ROO-01")))
                .andExpect(jsonPath("$.data.femaleChickenCode", is("HEN-01")));

        // Auto assignment of pairId verification
        Chicken updatedMale = chickenRepository.findById(validMale.getId()).orElseThrow();
        Chicken updatedFemale = chickenRepository.findById(validFemale.getId()).orElseThrow();
        assertNotNull(updatedMale.getPairId());
        assertNotNull(updatedFemale.getPairId());
        assertEquals(updatedMale.getPairId(), updatedFemale.getPairId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInvalidGenderAssignment_Rejected() throws Exception {
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-INVALID-GENDER")
                .maleChickenId(invalidMaleGender.getId()) // gender is female
                .femaleChickenId(validFemale.getId())
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Male chicken gender must be MALE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInactiveChicken_Rejected() throws Exception {
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-INACTIVE")
                .maleChickenId(validMale.getId())
                .femaleChickenId(inactiveChicken.getId()) // SOLD status
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Female chicken status must be ACTIVE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSameChickenSelected_Rejected() throws Exception {
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-SAME")
                .maleChickenId(validMale.getId())
                .femaleChickenId(validMale.getId()) // Same ID
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Male and female chickens cannot be the same")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateActivePair_Rejected() throws Exception {
        BreedingPairRequest firstRequest = BreedingPairRequest.builder()
                .pairCode("PAIR-FIRST")
                .maleChickenId(validMale.getId())
                .femaleChickenId(validFemale.getId())
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        // Create first active pair successfully
        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Create second active pair with same male -> should fail
        Chicken otherFemale = Chicken.builder()
                .chickenCode("HEN-02")
                .breed(Breed.ROSS_308)
                .category(ChickenCategory.BREEDER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(290))
                .status(ChickenStatus.ACTIVE)
                .weight(2.1)
                .build();
        otherFemale = chickenRepository.save(otherFemale);

        BreedingPairRequest secondRequest = BreedingPairRequest.builder()
                .pairCode("PAIR-SECOND")
                .maleChickenId(validMale.getId()) // Same male!
                .femaleChickenId(otherFemale.getId())
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Male chicken is already assigned to an active pair")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testStatusUpdates_AndAutoAssignmentRemoval() throws Exception {
        // 1. Create ACTIVE pair
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-MUTATION")
                .maleChickenId(validMale.getId())
                .femaleChickenId(validFemale.getId())
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.GENETIC_IMPROVEMENT)
                .build();

        String responseJson = mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long pairId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // Chickens have pairId assigned
        Chicken updatedMale = chickenRepository.findById(validMale.getId()).orElseThrow();
        assertEquals(pairId, updatedMale.getPairId());

        // 2. Perform patch update: Transition active pair to COMPLETED
        PairStatusUpdateRequest statusRequest = new PairStatusUpdateRequest(PairStatus.COMPLETED, LocalDate.now());

        mockMvc.perform(patch("/pairs/" + pairId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));

        // Chickens should have pairId cleared (null)
        Chicken clearedMale = chickenRepository.findById(validMale.getId()).orElseThrow();
        assertNull(clearedMale.getPairId());
        Chicken clearedFemale = chickenRepository.findById(validFemale.getId()).orElseThrow();
        assertNull(clearedFemale.getPairId());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testAuthorization_WorkerReadAllowedWriteBlocked() throws Exception {
        BreedingPairRequest request = BreedingPairRequest.builder()
                .pairCode("PAIR-FORBIDDEN")
                .maleChickenId(validMale.getId())
                .femaleChickenId(validFemale.getId())
                .startDate(LocalDate.now())
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .build();

        // Write is BLOCKED structure
        mockMvc.perform(post("/pairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Read is ALLOWED structure
        mockMvc.perform(get("/pairs")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilteringAndPagination() throws Exception {
        BreedingPair pair = BreedingPair.builder()
                .pairCode("GRP-PAIR-01")
                .maleChicken(validMale)
                .femaleChicken(validFemale)
                .startDate(LocalDate.now().minusDays(10))
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.SELECTIVE_BREEDING)
                .build();
        breedingPairRepository.save(pair);

        mockMvc.perform(get("/pairs")
                        .param("maleChickenId", validMale.getId().toString())
                        .param("status", "ACTIVE")
                        .param("purpose", "SELECTIVE_BREEDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].pairCode", is("GRP-PAIR-01")));
    }
}
