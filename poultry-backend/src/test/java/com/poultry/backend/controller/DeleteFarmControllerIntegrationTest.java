package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.DeleteFarmRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DeleteFarmControllerIntegrationTest {

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

        // 1. Create Owner User
        ownerUser = User.builder()
                .fullName("Owner Sudhakar")
                .email("owner.test@example.com")
                .phoneNumber("+919999999999")
                .password(passwordEncoder.encode("OwnerPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        // 2. Create Worker User
        workerUser = User.builder()
                .fullName("Worker John")
                .email("worker.john@example.com")
                .phoneNumber("+918888888888")
                .password(passwordEncoder.encode("WorkerPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        workerUser = userRepository.save(workerUser);
        workerToken = jwtUtils.generateToken(new CustomUserDetails(workerUser));

        // 3. Create Test Farm with Primary Owner membership
        testFarm = Farm.builder()
                .name("Greenfield Poultry Farm")
                .farmUniqueId("FARM-TEST-001")
                .joinCode("JOIN1234")
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
    void testDeleteCheck_Owner_NoWorkers_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/delete-check", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.canDelete", is(true)))
                .andExpect(jsonPath("$.data.workerCount", is(0)));
    }

    @Test
    void testDeleteCheck_Owner_WorkersConnected_Returns409Conflict() throws Exception {
        // Add a worker to the farm
        FarmMember workerMembership = FarmMember.builder()
                .farm(testFarm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(workerMembership);

        mockMvc.perform(get("/api/v1/farms/{farmId}/delete-check", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Workers are still connected")));
    }

    @Test
    void testDeleteFarm_NonOwner_Returns403Forbidden() throws Exception {
        FarmMember workerMembership = FarmMember.builder()
                .farm(testFarm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(workerMembership);

        DeleteFarmRequest request = DeleteFarmRequest.builder()
                .confirmationText("DELETE")
                .password("WorkerPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/delete", testFarm.getId())
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Only the primary farm owner can delete")));
    }

    @Test
    void testDeleteFarm_WrongPassword_Returns401Unauthorized() throws Exception {
        DeleteFarmRequest request = DeleteFarmRequest.builder()
                .confirmationText("DELETE")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/delete", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Incorrect password.")));
    }

    @Test
    void testDeleteFarm_InvalidConfirmationText_Returns400BadRequest() throws Exception {
        DeleteFarmRequest request = DeleteFarmRequest.builder()
                .confirmationText("delete") // lowercase invalid
                .password("OwnerPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/delete", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Confirmation text must match 'DELETE'")));
    }

    @Test
    void testDeleteFarm_WorkersConnected_Returns409Conflict() throws Exception {
        // Add a worker to the farm
        FarmMember workerMembership = FarmMember.builder()
                .farm(testFarm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(workerMembership);

        DeleteFarmRequest request = DeleteFarmRequest.builder()
                .confirmationText("DELETE")
                .password("OwnerPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/delete", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Workers are still connected")));
    }

    @Test
    void testDeleteFarm_Success_RollsUpAndDeletesEverything() throws Exception {
        DeleteFarmRequest request = DeleteFarmRequest.builder()
                .confirmationText("DELETE")
                .password("OwnerPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/delete", testFarm.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.message", is("Farm and owner account deleted successfully.")));

        // Verify farm and owner user are completely removed from database
        assertFalse(farmRepository.existsById(testFarm.getId()));
        assertFalse(userRepository.existsById(ownerUser.getId()));
    }
}
