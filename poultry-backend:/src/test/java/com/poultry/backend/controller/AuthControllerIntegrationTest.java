package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testAuthenticationAndRegistrationWorkflow() throws Exception {
        // ==========================================
        // 1. OWNER REGISTRATION FLOW
        // ==========================================
        OwnerRegisterRequest ownerRequest = OwnerRegisterRequest.builder()
                .fullName("Owner David")
                .email("owner.david@example.com")
                .phoneNumber("+380997777777")
                .password("StrongPassword123!")
                .farmName("Golden Egg Farm")
                .build();

        mockMvc.perform(post("/auth/register/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("owner.david@example.com")));

        // Query database to fetch owner user and verify verification token details
        User dbOwner = userRepository.findByEmail("owner.david@example.com").orElseThrow();
        assertFalse(dbOwner.isEmailVerified());
        assertNotNull(dbOwner.getEmailVerificationToken());
        assertEquals("Golden Egg Farm", dbOwner.getPendingFarmName());

        // Validate: duplicate email registration prevention
        mockMvc.perform(post("/auth/register/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));

        // Validate: login rejection before email verification
        LoginRequest badLoginRequest = new LoginRequest("owner.david@example.com", "StrongPassword123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Email is not verified")));

        // ==========================================
        // 2. OWNER EMAIL VERIFICATION & FARM CREATION
        // ==========================================
        EmailVerificationRequest ownerVerification = new EmailVerificationRequest(
                dbOwner.getEmail(),
                dbOwner.getEmailVerificationToken()
        );

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerVerification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify database status: Email verified, pending farm name cleared
        dbOwner = userRepository.findByEmail("owner.david@example.com").orElseThrow();
        assertTrue(dbOwner.isEmailVerified());
        assertNull(dbOwner.getPendingFarmName());

        // Verify database: Farm automatically created, owner has APPROVED PRIMARY_OWNER membership
        Optional<FarmMember> ownerMembershipOpt = farmMemberRepository.findByUserId(dbOwner.getId()).stream().findFirst();
        assertTrue(ownerMembershipOpt.isPresent());
        FarmMember ownerMembership = ownerMembershipOpt.get();
        assertEquals(FarmRole.PRIMARY_OWNER, ownerMembership.getRole());
        assertEquals(MembershipStatus.APPROVED, ownerMembership.getStatus());

        Farm createdFarm = ownerMembership.getFarm();
        assertNotNull(createdFarm.getFarmUniqueId());
        assertNotNull(createdFarm.getJoinCode());
        assertEquals("Golden Egg Farm", createdFarm.getName());

        // Login as Owner: Success case
        String ownerLoginResponseJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String ownerJwtToken = objectMapper.readTree(ownerLoginResponseJson).path("data").path("token").asText();

        // ==========================================
        // 3. WORKER REGISTRATION & FIELD VALIDATIONS
        // ==========================================
        WorkerRegisterRequest workerRequest = WorkerRegisterRequest.builder()
                .fullName("Worker James")
                .email("worker.james@example.com")
                .phoneNumber("+380998888888")
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post("/auth/register/worker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        User dbWorker = userRepository.findByEmail("worker.james@example.com").orElseThrow();
        assertFalse(dbWorker.isEmailVerified());

        // Validate: Join Farm rejected before email verification
        JoinFarmRequest preVerificationJoin = JoinFarmRequest.builder()
                .email("worker.james@example.com")
                .farmUniqueId(createdFarm.getFarmUniqueId())
                .joinCode(createdFarm.getJoinCode())
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/auth/join-farm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preVerificationJoin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("verify your email")));

        // ==========================================
        // 4. WORKER EMAIL VERIFICATION
        // ==========================================
        EmailVerificationRequest workerVerification = new EmailVerificationRequest(
                dbWorker.getEmail(),
                dbWorker.getEmailVerificationToken()
        );

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerVerification)))
                .andExpect(status().isOk());

        dbWorker = userRepository.findByEmail("worker.james@example.com").orElseThrow();
        assertTrue(dbWorker.isEmailVerified());

        // ==========================================
        // 5. WORKER JOIN FARM REQUEST (STEP 3 & 4)
        // ==========================================
        // Invalid Farm Unique ID validation
        JoinFarmRequest badJoinRequest = JoinFarmRequest.builder()
                .email("worker.james@example.com")
                .farmUniqueId("WRONG-UUID-12345")
                .joinCode(createdFarm.getJoinCode())
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/auth/join-farm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badJoinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid Farm Unique ID")));

        // Valid Join Farm Submission
        JoinFarmRequest goodJoinRequest = JoinFarmRequest.builder()
                .email("worker.james@example.com")
                .farmUniqueId(createdFarm.getFarmUniqueId())
                .joinCode(createdFarm.getJoinCode())
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/auth/join-farm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodJoinRequest)))
                .andExpect(status().isOk());

        // Verify status PENDING in database
        FarmMember workerMembership = farmMemberRepository.findByFarmIdAndUserId(createdFarm.getId(), dbWorker.getId()).orElseThrow();
        assertEquals(FarmRole.WORKER, workerMembership.getRole());
        assertEquals(MembershipStatus.PENDING, workerMembership.getStatus());

        // Prevent Duplicate requests
        mockMvc.perform(post("/auth/join-farm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodJoinRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Duplicate Join Request")));

        // ==========================================
        // 6. DASHBOARD BLOCKING FOR PENDING USER
        // ==========================================
        LoginRequest workerLogin = new LoginRequest("worker.james@example.com", "StrongPassword123!");
        String workerLoginJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workerJwtToken = objectMapper.readTree(workerLoginJson).path("data").path("token").asText();

        // Worker accesses farm members list while PENDING
        mockMvc.perform(get("/api/v2/farms/" + createdFarm.getFarmUniqueId() + "/members")
                        .header("Authorization", "Bearer " + workerJwtToken))
                .andExpect(status().isForbidden()); // Blocked: membership is PENDING, not APPROVED

        // ==========================================
        // 7. APPROVAL & SUCCESSFUL ACCESS
        // ==========================================
        // Owner approves worker
        mockMvc.perform(post("/api/v2/farms/members/" + workerMembership.getId() + "/approve")
                        .header("Authorization", "Bearer " + ownerJwtToken))
                .andExpect(status().isOk());

        // Access dashboard endpoints after approval: succeeds
        mockMvc.perform(get("/api/v2/farms/" + createdFarm.getFarmUniqueId() + "/members")
                        .header("Authorization", "Bearer " + workerJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
