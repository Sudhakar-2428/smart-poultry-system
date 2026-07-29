package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.FarmRequest;
import com.poultry.backend.dto.JoinRequest;
import com.poultry.backend.dto.RoleUpdateRequest;
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
public class FarmControllerIntegrationTest {

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
    private User applicantUser;
    private User otherApplicantUser;
    private String ownerToken;
    private String applicantToken;
    private String otherApplicantToken;

    @BeforeEach
    void setUp() {
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Owner User
        ownerUser = User.builder()
                .fullName("Owner Jack")
                .email("owner.jack@example.com")
                .phoneNumber("+380991111111")
                .password(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtUtils.generateToken(new CustomUserDetails(ownerUser));

        // 2. Create applicant User
        applicantUser = User.builder()
                .fullName("Applicant Tim")
                .email("app.tim@example.com")
                .phoneNumber("+380992222222")
                .password(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        applicantUser = userRepository.save(applicantUser);
        applicantToken = jwtUtils.generateToken(new CustomUserDetails(applicantUser));

        // 3. Create other applicant User
        otherApplicantUser = User.builder()
                .fullName("Applicant Sarah")
                .email("app.sarah@example.com")
                .phoneNumber("+380993333333")
                .password(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        otherApplicantUser = userRepository.save(otherApplicantUser);
        otherApplicantToken = jwtUtils.generateToken(new CustomUserDetails(otherApplicantUser));
    }

    @Test
    void testFarmWorkflow_Success() throws Exception {
        // --- 1. Farm Creation ---
        FarmRequest creationRequest = FarmRequest.builder().name("Green Field Poultry").build();

        String responseJson = mockMvc.perform(post("/api/v2/farms")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Green Field Poultry")))
                .andExpect(jsonPath("$.data.farmUniqueId", notNullValue()))
                .andExpect(jsonPath("$.data.joinCode", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Get details from JSON
        String farmUniqueId = objectMapper.readTree(responseJson).path("data").path("farmUniqueId").asText();
        String joinCode = objectMapper.readTree(responseJson).path("data").path("joinCode").asText();
        Long farmId = objectMapper.readTree(responseJson).path("data").path("id").asLong();

        // Verify database state: Owner should be registered as APPROVED PRIMARY_OWNER
        boolean hasOwner = farmMemberRepository.existsByFarmIdAndUserId(farmId, ownerUser.getId());
        assertTrue(hasOwner);
        FarmMember ownerMember = farmMemberRepository.findByFarmIdAndUserId(farmId, ownerUser.getId()).orElseThrow();
        assertEquals(FarmRole.PRIMARY_OWNER, ownerMember.getRole());
        assertEquals(MembershipStatus.APPROVED, ownerMember.getStatus());

        // --- 2. Retrieve My Farms ---
        mockMvc.perform(get("/api/v2/farms/my-farm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].farmUniqueId", is(farmUniqueId)));

        // --- 3. Join Request: Join Code Validation Failure ---
        JoinRequest badJoinCodeRequest = new JoinRequest("INVALID999", FarmRole.WORKER);
        mockMvc.perform(post("/api/v2/farms/join-request")
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badJoinCodeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid farm join code")));

        // --- 4. Join Request: Success Case ---
        JoinRequest goodJoinRequest = new JoinRequest(joinCode, FarmRole.WORKER);
        String joinResponseJson = mockMvc.perform(post("/api/v2/farms/join-request")
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodJoinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.role", is("WORKER")))
                .andReturn().getResponse().getContentAsString();

        Long timMemberId = objectMapper.readTree(joinResponseJson).path("data").path("id").asLong();

        // --- 5. Duplicate Join Prevention ---
        mockMvc.perform(post("/api/v2/farms/join-request")
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodJoinRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already")));

        // --- 6. Load Pending Requests ---
        mockMvc.perform(get("/api/v2/farms/pending-members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(timMemberId.intValue())));

        // --- 7. Approval Workflow ---
        mockMvc.perform(post("/api/v2/farms/members/" + timMemberId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // --- 8. Rejection Workflow ---
        // Sarah sends join request
        JoinRequest sarahJoinRequest = new JoinRequest(joinCode, FarmRole.MANAGER);
        String sarahJoinJson = mockMvc.perform(post("/api/v2/farms/join-request")
                        .header("Authorization", "Bearer " + otherApplicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sarahJoinRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long sarahMemberId = objectMapper.readTree(sarahJoinJson).path("data").path("id").asLong();

        // Owner rejects Sarah
        mockMvc.perform(post("/api/v2/farms/members/" + sarahMemberId + "/reject")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REJECTED")));

        // --- 9. Role Changes ---
        RoleUpdateRequest updateRoleRequest = new RoleUpdateRequest(FarmRole.CO_OWNER);
        mockMvc.perform(put("/api/v2/farms/members/" + timMemberId + "/role")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRoleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("CO_OWNER")));
    }
}
