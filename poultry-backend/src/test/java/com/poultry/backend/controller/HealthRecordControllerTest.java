package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.HealthRecordRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.BreedingPairRepository;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.HealthRecordRepository;
import com.poultry.backend.repository.NotificationRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HealthRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private BreedingPairRepository breedingPairRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Chicken testChicken;
    private Chicken partnerChicken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        healthRecordRepository.deleteAll();
        breedingPairRepository.deleteAll();
        chickenRepository.deleteAll();

        // Create standard healthy active birds for testing
        testChicken = Chicken.builder()
                .chickenCode("CHK-HLTH-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(180))
                .status(ChickenStatus.ACTIVE)
                .weight(1.8)
                .build();
        testChicken = chickenRepository.save(testChicken);

        partnerChicken = Chicken.builder()
                .chickenCode("CHK-HLTH-02")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.ROOSTER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(190))
                .status(ChickenStatus.ACTIVE)
                .weight(2.4)
                .build();
        partnerChicken = chickenRepository.save(partnerChicken);
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testCreateVaccination_Success() throws Exception {
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-VAC-01")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Newcastle Disease Vaccine")
                .vaccinationBatch("BATCH-A9")
                .nextVaccinationDate(LocalDate.now().plusDays(30))
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vaccinationName", is("Newcastle Disease Vaccine")))
                .andExpect(jsonPath("$.data.healthStatus", is("HEALTHY")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testDuplicateVaccinationSameDay_Rejected() throws Exception {
        // Create first vaccination record
        HealthRecordRequest firstRequest = HealthRecordRequest.builder()
                .recordCode("REC-VAC-FIRST")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Marek's Vaccine")
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Attempt secondary duplicate vaccine on same day
        HealthRecordRequest secondRequest = HealthRecordRequest.builder()
                .recordCode("REC-VAC-SECOND")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now()) // same day
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Marek's Vaccine") // same vaccine name
                .veterinarian("Dr. Jones")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Duplicate vaccination")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testCreateTreatment_Success() throws Exception {
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-TRT-01")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.TREATMENT)
                .diseaseName("Coccidiosis")
                .medicineName("Amprolium")
                .medicineDose("9.6% solution in water")
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.UNDER_TREATMENT)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.healthType", is("TREATMENT")))
                .andExpect(jsonPath("$.data.medicineName", is("Amprolium")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testFutureRecordDate_Rejected() throws Exception {
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-FUTURE")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now().plusDays(1)) // future
                .healthType(HealthType.CHECKUP)
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Record date cannot be in the future")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testInvalidVaccinationDate_Rejected() throws Exception {
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-INV-DATE")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Newcastle Vaccine")
                .nextVaccinationDate(LocalDate.now().minusDays(1)) // before record date
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Next vaccination date must be greater than or equal")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testMarkChickenDeceased_AutoStatusUpdate_AndPairRemoval() throws Exception {
        // Create active breeding pair for these two chickens
        BreedingPair pair = BreedingPair.builder()
                .pairCode("PAIR-DEATH-TEST")
                .maleChicken(partnerChicken)
                .femaleChicken(testChicken)
                .startDate(LocalDate.now().minusDays(10))
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.GENETIC_IMPROVEMENT)
                .build();
        pair = breedingPairRepository.save(pair);

        // Link pairId to chickens
        testChicken.setPairId(pair.getId());
        chickenRepository.save(testChicken);
        partnerChicken.setPairId(pair.getId());
        chickenRepository.save(partnerChicken);

        // Record a deceased health entry
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-DEATH")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.DISEASE)
                .diseaseName("Avian Influenza")
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.DECEASED)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mortality", is(true)));

        // 1. Check chicken is set to DEAD
        Chicken deathCheck = chickenRepository.findById(testChicken.getId()).orElseThrow();
        assertEquals(ChickenStatus.DEAD, deathCheck.getStatus());
        assertNull(deathCheck.getPairId());

        // 2. Check Breeding Pair became COMPLETED and cleared partner's pairId
        BreedingPair pairCheck = breedingPairRepository.findById(pair.getId()).orElseThrow();
        assertEquals(PairStatus.COMPLETED, pairCheck.getStatus());

        Chicken partnerCheck = chickenRepository.findById(partnerChicken.getId()).orElseThrow();
        assertNull(partnerCheck.getPairId());

        // 3. Deceased chicken cannot receive future health records
        HealthRecordRequest subsequentRequest = HealthRecordRequest.builder()
                .recordCode("REC-SUBSEQUENT")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.CHECKUP)
                .veterinarian("Dr. Jones")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subsequentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("A deceased chicken cannot receive additional health records")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testNotificationCreation() throws Exception {
        // 1. Vaccination due within 3 days
        HealthRecordRequest dueRequest = HealthRecordRequest.builder()
                .recordCode("REC-NOTIF-DUE")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Fowl Pox")
                .nextVaccinationDate(LocalDate.now().plusDays(2)) // 2 days in future
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dueRequest)))
                .andExpect(status().isCreated());

        List<Notification> dues = notificationRepository.findByType("VACCINATION_DUE");
        assertFalse(dues.isEmpty());
        assertTrue(dues.get(0).getMessage().contains("Fowl Pox"));

        // 2. Vaccination overdue
        HealthRecordRequest overdueRequest = HealthRecordRequest.builder()
                .recordCode("REC-NOTIF-OVERDUE")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now().minusDays(5))
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Fowl Pox Overdue")
                .nextVaccinationDate(LocalDate.now().minusDays(1)) // in the past
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdueRequest)))
                .andExpect(status().isCreated());

        List<Notification> overdues = notificationRepository.findByType("VACCINATION_OVERDUE");
        assertFalse(overdues.isEmpty());

        // 3. Disease Critical
        HealthRecordRequest criticalRequest = HealthRecordRequest.builder()
                .recordCode("REC-NOTIF-CRIT")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.DISEASE)
                .diseaseName("Coryza")
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.CRITICAL)
                .build();

        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criticalRequest)))
                .andExpect(status().isCreated());

        List<Notification> criticals = notificationRepository.findByType("CRITICAL_DISEASE");
        assertFalse(criticals.isEmpty());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testAuthorization_WorkerReadAllowedWriteBlocked() throws Exception {
        HealthRecordRequest request = HealthRecordRequest.builder()
                .recordCode("REC-WRK-FORBIDDEN")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.CHECKUP)
                .veterinarian("Dr. Worker")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        // Write is BLOCKED
        mockMvc.perform(post("/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Read is ALLOWED
        mockMvc.perform(get("/health-records")
                        .param("healthStatus", "HEALTHY"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilteringAndPagination() throws Exception {
        HealthRecord record = HealthRecord.builder()
                .recordCode("GRP-REC-01")
                .chicken(testChicken)
                .recordDate(LocalDate.now().minusDays(5))
                .healthType(HealthType.CHECKUP)
                .veterinarian("Dr. Watson")
                .healthStatus(HealthStatus.HEALTHY)
                .build();
        healthRecordRepository.save(record);

        mockMvc.perform(get("/health-records")
                        .param("chickenId", testChicken.getId().toString())
                        .param("healthType", "CHECKUP")
                        .param("veterinarian", "Watson")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].recordCode", is("GRP-REC-01")));
    }
}
