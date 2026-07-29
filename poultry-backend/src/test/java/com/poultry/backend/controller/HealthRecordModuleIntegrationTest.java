package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.HealthRecordRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.HealthRecordRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class HealthRecordModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Chicken testChicken;

    @BeforeEach
    void setUp() {
        healthRecordRepository.deleteAll();
        chickenRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .fullName("Vet Admin")
                .email("vet.admin@example.com")
                .phoneNumber("+15559990000")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        userRepository.save(admin);

        CustomUserDetails userDetails = new CustomUserDetails(admin, "VETERINARIAN");
        adminToken = jwtUtils.generateToken(userDetails);

        testChicken = Chicken.builder()
                .chickenCode("CHK-HLT-001")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .healthStatus(HealthStatus.HEALTHY)
                .status(ChickenStatus.ACTIVE)
                .dateOfBirth(LocalDate.now().minusDays(45))
                .build();
        chickenRepository.save(testChicken);
    }

    @Test
    void testGetDashboardStats_Success() throws Exception {
        mockMvc.perform(get("/api/v1/health-records/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.healthy", is(1)));
    }

    @Test
    void testGetReminders_Success() throws Exception {
        HealthRecord overdue = HealthRecord.builder()
                .recordCode("REC-OVERDUE-01")
                .chicken(testChicken)
                .recordDate(LocalDate.now().minusDays(10))
                .healthType(HealthType.VACCINATION)
                .vaccinationName("ND Vaccine")
                .nextVaccinationDate(LocalDate.now().minusDays(2))
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();
        healthRecordRepository.save(overdue);

        mockMvc.perform(get("/api/v1/health-records/reminders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.overdueVaccinations", hasSize(1)));
    }

    @Test
    void testCreateHealthRecord_Success() throws Exception {
        HealthRecordRequest req = HealthRecordRequest.builder()
                .recordCode("REC-TEST-001")
                .chickenId(testChicken.getId())
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("IB Vaccine")
                .vaccinationBatch("BAT-999")
                .manufacturer("PoultryPharm")
                .administeredBy("Dr. Smith")
                .nextVaccinationDate(LocalDate.now().plusDays(21))
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build();

        mockMvc.perform(post("/api/v1/health-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.recordCode", is("REC-TEST-001")));
    }
}
