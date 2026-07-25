package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.JoinFarmTempRequest;
import com.poultry.backend.dto.WorkerInviteRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.NotificationRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class WorkerInvitationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMemberRepository farmMemberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private User ownerUser;
    private Farm testFarm;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Owner User
        ownerUser = User.builder()
                .fullName("Owner Jack")
                .email("owner.inviter@example.com")
                .phoneNumber("+15551112222")
                .password(passwordEncoder.encode("OwnerPass123!"))
                .role(Role.MANAGER)
                .isActive(true)
                .emailVerified(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        // 2. Create Test Farm with Primary Owner membership
        testFarm = Farm.builder()
                .name("Greenfield Hatchery")
                .farmUniqueId("FARM-INV-001")
                .joinCode("JOIN9999")
                .build();
        testFarm = farmRepository.save(testFarm);

        FarmMember ownerMembership = FarmMember.builder()
                .farm(testFarm)
                .user(ownerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(ownerMembership);
    }

    @Test
    void testInviteWorker_Success_GeneratesTempPasswordAndPendingStatus() throws Exception {
        WorkerInviteRequest inviteRequest = WorkerInviteRequest.builder()
                .fullName("Invited Worker Bob")
                .email("bob.worker@example.com")
                .phoneNumber("+15553334444")
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/workers/invite", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Invited Worker Bob")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.temporaryPassword", notNullValue()))
                .andExpect(jsonPath("$.data.workerId", startsWith("WRK-")));
    }

    @Test
    void testJoinFarmWithTempPassword_Success_ActivatesWorkerAndSendsNotifications() throws Exception {
        // Step 1: Owner invites worker
        WorkerInviteRequest inviteRequest = WorkerInviteRequest.builder()
                .fullName("Worker Alice")
                .email("alice.worker@example.com")
                .phoneNumber("+15558889999")
                .role(FarmRole.WORKER)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/farms/{farmId}/workers/invite", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String tempPassword = objectMapper.readTree(jsonResponse).path("data").path("temporaryPassword").asText();
        String workerIdStr = objectMapper.readTree(jsonResponse).path("data").path("workerId").asText();

        // Step 2: Worker joins farm using join-farm.html credentials
        JoinFarmTempRequest joinRequest = JoinFarmTempRequest.builder()
                .farmId(testFarm.getId().toString())
                .workerId(workerIdStr)
                .temporaryPassword(tempPassword)
                .newPassword("PermanentPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/join-farm-temp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("alice.worker@example.com")));

        // Step 3: Verify worker status updated to APPROVED in DB
        User workerUser = userRepository.findByEmail("alice.worker@example.com").orElseThrow();
        FarmMember member = farmMemberRepository.findByFarmIdAndUserId(testFarm.getId(), workerUser.getId()).orElseThrow();
        assertEquals(MembershipStatus.APPROVED, member.getStatus());
        assertFalse(workerUser.isMustChangePassword());

        // Step 4: Verify notifications created for owner and worker
        List<Notification> notifications = notificationRepository.findAll();
        assertTrue(notifications.stream().anyMatch(n -> n.getTitle().contains("Worker Joined Successfully")));
        assertTrue(notifications.stream().anyMatch(n -> n.getTitle().contains("Welcome to Greenfield Hatchery")));
    }

    @Test
    void testJoinFarmWithTempPassword_InvalidPassword_Returns401() throws Exception {
        WorkerInviteRequest inviteRequest = WorkerInviteRequest.builder()
                .fullName("Worker Charlie")
                .email("charlie.worker@example.com")
                .phoneNumber("+15557776666")
                .role(FarmRole.WORKER)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/farms/{farmId}/workers/invite", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String workerIdStr = objectMapper.readTree(jsonResponse).path("data").path("workerId").asText();

        JoinFarmTempRequest joinRequest = JoinFarmTempRequest.builder()
                .farmId(testFarm.getId().toString())
                .workerId(workerIdStr)
                .temporaryPassword("InvalidTempPass")
                .newPassword("PermanentPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/join-farm-temp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid temporary password")));
    }
}
