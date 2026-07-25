package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.FarmProfileUpdateRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FarmProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMemberRepository farmMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private User ownerUser;
    private User workerUser;
    private Farm testFarm;
    private String ownerToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Owner User
        ownerUser = User.builder()
                .fullName("Farm Owner Sam")
                .email("owner.sam@example.com")
                .phoneNumber("+15550001111")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.MANAGER)
                .isActive(true)
                .emailVerified(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        // 2. Worker User
        workerUser = User.builder()
                .fullName("Worker Dan")
                .email("worker.dan@example.com")
                .phoneNumber("+15550002222")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.WORKER)
                .isActive(true)
                .emailVerified(true)
                .build();
        workerUser = userRepository.save(workerUser);
        workerToken = jwtUtils.generateToken(new CustomUserDetails(workerUser));

        // 3. Farm
        testFarm = Farm.builder()
                .name("Sunrise Organic Farm")
                .farmUniqueId("FARM-PROF-001")
                .joinCode("JOIN1234")
                .farmAddress("123 Poultry Lane")
                .village("Green Valley")
                .district("Central District")
                .state("California")
                .country("USA")
                .pinCode("90210")
                .latitude(34.0522)
                .longitude(-118.2437)
                .build();
        testFarm = farmRepository.save(testFarm);

        // Memberships
        FarmMember ownerMember = FarmMember.builder()
                .farm(testFarm)
                .user(ownerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(ownerMember);

        FarmMember workerMember = FarmMember.builder()
                .farm(testFarm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(workerMember);
    }

    @Test
    void testGetFarmProfile_Success_ReturnsProfileDetails() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/profile", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.farmName", is("Sunrise Organic Farm")))
                .andExpect(jsonPath("$.data.ownerName", is("Farm Owner Sam")))
                .andExpect(jsonPath("$.data.village", is("Green Valley")))
                .andExpect(jsonPath("$.data.totalWorkers", is(1)));
    }

    @Test
    void testUpdateFarmProfile_AsPrimaryOwner_Success() throws Exception {
        FarmProfileUpdateRequest updateRequest = FarmProfileUpdateRequest.builder()
                .farmName("Sunrise Mega Farm")
                .email("info@sunrisemega.com")
                .phone("+15559998888")
                .farmAddress("456 Mega Poultry Way")
                .village("New Valley")
                .district("North District")
                .state("California")
                .country("USA")
                .pinCode("90211")
                .latitude(34.1000)
                .longitude(-118.3000)
                .build();

        mockMvc.perform(put("/api/v1/farms/{farmId}/profile", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.farmName", is("Sunrise Mega Farm")))
                .andExpect(jsonPath("$.data.email", is("info@sunrisemega.com")))
                .andExpect(jsonPath("$.data.pinCode", is("90211")));
    }

    @Test
    void testUpdateFarmProfile_AsWorker_Returns403Forbidden() throws Exception {
        FarmProfileUpdateRequest updateRequest = FarmProfileUpdateRequest.builder()
                .farmName("Unauthorized Name Change")
                .build();

        mockMvc.perform(put("/api/v1/farms/{farmId}/profile", testFarm.getId())
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUploadAndRemoveFarmLogo_Success() throws Exception {
        MockMultipartFile logoFile = new MockMultipartFile(
                "file",
                "farm_logo.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy logo content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/farms/{farmId}/logo", testFarm.getId())
                .file(logoFile)
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.logoUrl", startsWith("data:image/png;base64,")));

        // Remove Logo
        mockMvc.perform(delete("/api/v1/farms/{farmId}/logo", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.logoUrl", nullValue()));
    }
}
