package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenStatusPatchRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
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
public class ChickenProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ChickenTimelineRepository chickenTimelineRepository;

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
        chickenTimelineRepository.deleteAll();
        chickenRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .fullName("Profile Admin")
                .email("profile.admin@example.com")
                .phoneNumber("+15557778888")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        userRepository.save(admin);

        CustomUserDetails userDetails = new CustomUserDetails(admin, "PRIMARY_OWNER");
        adminToken = jwtUtils.generateToken(userDetails);

        testChicken = Chicken.builder()
                .chickenCode("CHK-000201")
                .name("Speedy")
                .category(ChickenCategory.COUNTRY_CHICKEN)
                .breed(Breed.LEGHORN)
                .gender(Gender.FEMALE)
                .weight(1.5)
                .healthStatus(HealthStatus.HEALTHY)
                .status(ChickenStatus.ACTIVE)
                .dateOfBirth(LocalDate.now().minusDays(120))
                .origin(ChickenOrigin.FARM_BORN)
                .wingTagNumber("WING-201")
                .legBandNumber("LEG-201")
                .build();
        chickenRepository.save(testChicken);
    }

    @Test
    void testGetChickenProfile_Success() throws Exception {
        mockMvc.perform(get("/api/v1/chickens/" + testChicken.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.chickenCode", is("CHK-000201")))
                .andExpect(jsonPath("$.data.weight", is(1.5)))
                .andExpect(jsonPath("$.data.healthStatus", is("HEALTHY")));
    }

    @Test
    void testPatchStatus_Success() throws Exception {
        ChickenStatusPatchRequest patchRequest = ChickenStatusPatchRequest.builder()
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.SICK)
                .remarks("Observed sluggish behavior")
                .build();

        mockMvc.perform(patch("/api/v1/chickens/" + testChicken.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.healthStatus", is("SICK")));

        Chicken updated = chickenRepository.findById(testChicken.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(HealthStatus.SICK, updated.getHealthStatus());
    }

    @Test
    void testSoftDeleteChicken_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/chickens/" + testChicken.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("soft deleted successfully")));

        Chicken updated = chickenRepository.findById(testChicken.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ChickenStatus.INACTIVE, updated.getStatus());
    }

    @Test
    void testGetChickenTimeline_Success() throws Exception {
        mockMvc.perform(get("/api/v1/chickens/" + testChicken.getId() + "/timeline")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(notNullValue())));
    }
}
