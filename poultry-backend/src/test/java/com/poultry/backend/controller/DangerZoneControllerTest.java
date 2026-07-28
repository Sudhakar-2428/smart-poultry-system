package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.DangerZoneActionRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DangerZoneControllerTest {

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
                .fullName("Danger Owner")
                .email("danger.owner@example.com")
                .phoneNumber("+919999991111")
                .password(passwordEncoder.encode("OwnerPass123!"))
                .role(Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        // 2. Create Worker User
        workerUser = User.builder()
                .fullName("Danger Worker")
                .email("danger.worker@example.com")
                .phoneNumber("+918888882222")
                .password(passwordEncoder.encode("WorkerPass123!"))
                .role(Role.WORKER)
                .isActive(true)
                .emailVerified(true)
                .build();
        workerUser = userRepository.save(workerUser);
        workerToken = jwtUtils.generateToken(new CustomUserDetails(workerUser));

        // 3. Create Test Farm
        testFarm = Farm.builder()
                .name("Danger Zone Test Farm")
                .farmUniqueId("FARM-DANGER-001")
                .joinCode("DANGER12")
                .build();
        testFarm = farmRepository.save(testFarm);

        // 4. Primary Owner Membership
        FarmMember ownerMember = FarmMember.builder()
                .farm(testFarm)
                .user(ownerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(ownerMember);

        // 5. Worker Membership
        FarmMember workerMember = FarmMember.builder()
                .farm(testFarm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(workerMember);
    }

    @Test
    void testRemoveChickenData_OwnerSuccess() throws Exception {
        DangerZoneActionRequest request = DangerZoneActionRequest.builder()
                .confirmationText("DELETE ALL CHICKENS")
                .password("OwnerPass123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/danger-zone/chickens", testFarm.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("removed successfully")));
    }

    @Test
    void testRemoveChickenData_WorkerForbidden() throws Exception {
        DangerZoneActionRequest request = DangerZoneActionRequest.builder()
                .confirmationText("DELETE ALL CHICKENS")
                .password("WorkerPass123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/danger-zone/chickens", testFarm.getId())
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveChickenData_InvalidConfirmationText() throws Exception {
        DangerZoneActionRequest request = DangerZoneActionRequest.builder()
                .confirmationText("WRONG TEXT")
                .password("OwnerPass123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/danger-zone/chickens", testFarm.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResetSettings_OwnerSuccess() throws Exception {
        DangerZoneActionRequest request = DangerZoneActionRequest.builder()
                .confirmationText("RESET SETTINGS")
                .password("OwnerPass123!")
                .build();

        mockMvc.perform(post("/api/v1/farms/{farmId}/danger-zone/reset-settings", testFarm.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("restored successfully")));
    }

    @Test
    void testExportBackup_OwnerSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/danger-zone/export-backup", testFarm.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.farmName", is("Danger Zone Test Farm")));
    }
}
