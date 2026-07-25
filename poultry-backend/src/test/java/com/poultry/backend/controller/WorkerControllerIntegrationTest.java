package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.WorkerRequest;
import com.poultry.backend.dto.WorkerUpdateRequest;
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
public class WorkerControllerIntegrationTest {

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

    private Farm farm;
    private User ownerUser;
    private User workerUser;
    private FarmMember ownerMember;
    private FarmMember workerMember;
    private String ownerToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Farm
        farm = Farm.builder()
                .name("Green Farm")
                .farmUniqueId("GREEN-FARM-001")
                .joinCode("JOIN1234")
                .build();
        farm = farmRepository.save(farm);

        // 2. Create Owner User
        ownerUser = User.builder()
                .fullName("Farm Owner")
                .email("owner@greenfarm.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("OwnerPass123!"))
                .role(Role.MANAGER)
                .isActive(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        ownerMember = FarmMember.builder()
                .farm(farm)
                .user(ownerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        ownerMember = farmMemberRepository.save(ownerMember);

        // 3. Create Worker User
        workerUser = User.builder()
                .fullName("Existing Worker")
                .email("worker@greenfarm.com")
                .phoneNumber("+1987654321")
                .password(passwordEncoder.encode("WorkerPass123!"))
                .role(Role.WORKER)
                .isActive(true)
                .build();
        workerUser = userRepository.save(workerUser);
        workerToken = jwtUtils.generateToken(new CustomUserDetails(workerUser));

        workerMember = FarmMember.builder()
                .farm(farm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        workerMember = farmMemberRepository.save(workerMember);
    }

    @Test
    void testGetWorkers_Success() throws Exception {
        mockMvc.perform(get("/api/v2/farms/" + farm.getId() + "/workers")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[1].fullName", is("Existing Worker")))
                .andExpect(jsonPath("$.data[1].role", is("WORKER")));
    }

    @Test
    void testGetWorkers_ForbiddenForWorkerRole() throws Exception {
        mockMvc.perform(get("/api/v2/farms/" + farm.getId() + "/workers")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testCreateWorker_Success() throws Exception {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("John Doe")
                .email("john.doe@greenfarm.com")
                .phoneNumber("+1555666777")
                .password("TempPass123!")
                .role(FarmRole.WORKER)
                .build();

        String responseJson = mockMvc.perform(post("/api/v2/farms/" + farm.getId() + "/workers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.email", is("john.doe@greenfarm.com")))
                .andExpect(jsonPath("$.data.role", is("WORKER")))
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andReturn().getResponse().getContentAsString();

        // Verify user created in DB
        assertTrue(userRepository.existsByEmail("john.doe@greenfarm.com"));
    }

    @Test
    void testCreateWorker_DuplicateEmail_Conflict() throws Exception {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("Duplicate Email Person")
                .email("worker@greenfarm.com") // Already exists
                .phoneNumber("+1999888777")
                .password("TempPass123!")
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/api/v2/farms/" + farm.getId() + "/workers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void testCreateWorker_DuplicatePhone_Conflict() throws Exception {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("Duplicate Phone Person")
                .email("unique.email@greenfarm.com")
                .phoneNumber("+1987654321") // Already exists on workerUser
                .password("TempPass123!")
                .role(FarmRole.WORKER)
                .build();

        mockMvc.perform(post("/api/v2/farms/" + farm.getId() + "/workers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void testUpdateWorker_Success() throws Exception {
        WorkerUpdateRequest request = WorkerUpdateRequest.builder()
                .fullName("Existing Worker Updated")
                .role(FarmRole.MANAGER)
                .build();

        mockMvc.perform(put("/api/v2/farms/" + farm.getId() + "/workers/" + workerMember.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Existing Worker Updated")))
                .andExpect(jsonPath("$.data.role", is("MANAGER")));
    }

    @Test
    void testDeleteWorker_Success() throws Exception {
        mockMvc.perform(delete("/api/v2/farms/" + farm.getId() + "/workers/" + workerMember.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("removed successfully")));

        assertFalse(farmMemberRepository.existsById(workerMember.getId()));
    }

    @Test
    void testDeleteWorker_CannotDeleteOwner() throws Exception {
        mockMvc.perform(delete("/api/v2/farms/" + farm.getId() + "/workers/" + ownerMember.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot delete Farm Owner")));
    }

    @Test
    void testUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/v2/farms/" + farm.getId() + "/workers"))
                .andExpect(status().isUnauthorized());
    }
}
