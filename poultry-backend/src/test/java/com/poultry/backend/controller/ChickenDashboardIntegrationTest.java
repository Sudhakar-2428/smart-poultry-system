package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.BulkActionRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.ChickenRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChickenDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private Chicken testChicken1;
    private Chicken testChicken2;

    @BeforeEach
    void setUp() {
        chickenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test farm owner
        User owner = User.builder()
                .fullName("Dashboard Owner")
                .email("dash.owner@example.com")
                .phoneNumber("+15559998888")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build();
        userRepository.save(owner);

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        ownerToken = jwtUtils.generateToken(userDetails);

        // Create sample chickens
        testChicken1 = Chicken.builder()
                .chickenCode("CHK-000101")
                .name("Chitty")
                .category(ChickenCategory.COUNTRY_CHICKEN)
                .breed(Breed.LEGHORN)
                .gender(Gender.FEMALE)
                .weight(1.8)
                .healthStatus(HealthStatus.HEALTHY)
                .status(ChickenStatus.ACTIVE)
                .dateOfBirth(LocalDate.now().minusDays(100))
                .origin(ChickenOrigin.FARM_BORN)
                .wingTagNumber("WING-101")
                .legBandNumber("LEG-101")
                .build();
        chickenRepository.save(testChicken1);

        testChicken2 = Chicken.builder()
                .chickenCode("CHK-000102")
                .name("Rooster King")
                .category(ChickenCategory.BROILER)
                .breed(Breed.COBB_500)
                .gender(Gender.MALE)
                .weight(2.5)
                .healthStatus(HealthStatus.SICK)
                .status(ChickenStatus.ACTIVE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .origin(ChickenOrigin.PURCHASED)
                .wingTagNumber("WING-102")
                .legBandNumber("LEG-102")
                .build();
        chickenRepository.save(testChicken2);
    }

    @Test
    void testGetDashboardStats_Success() throws Exception {
        mockMvc.perform(get("/api/v1/chickens/stats")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalChickens", is(2)))
                .andExpect(jsonPath("$.data.healthy", is(1)))
                .andExpect(jsonPath("$.data.sick", is(1)))
                .andExpect(jsonPath("$.data.hens", is(1)))
                .andExpect(jsonPath("$.data.roosters", is(1)))
                .andExpect(jsonPath("$.data.countryChickens", is(1)))
                .andExpect(jsonPath("$.data.broilers", is(1)));
    }

    @Test
    void testSearchChickens_ByWingTagAndLegBand() throws Exception {
        // Search Wing Tag
        mockMvc.perform(get("/api/v1/chickens")
                        .param("search", "WING-101")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("CHK-000101")));

        // Search Leg Band
        mockMvc.perform(get("/api/v1/chickens")
                        .param("search", "LEG-102")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("CHK-000102")));
    }

    @Test
    void testFilterChickens_ByOriginAndHealth() throws Exception {
        mockMvc.perform(get("/api/v1/chickens")
                        .param("origin", "PURCHASED")
                        .param("healthStatus", "SICK")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("CHK-000102")));
    }

    @Test
    void testBulkArchive_Success() throws Exception {
        BulkActionRequest request = BulkActionRequest.builder()
                .ids(List.of(testChicken1.getId(), testChicken2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/chickens/bulk-archive")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Bulk archive completed successfully")));

        // Verify status updated in database
        Chicken updated1 = chickenRepository.findById(testChicken1.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ChickenStatus.SOLD, updated1.getStatus());
    }
}
