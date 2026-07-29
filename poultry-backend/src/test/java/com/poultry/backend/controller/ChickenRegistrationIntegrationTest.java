package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenVaccinationDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChickenRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private com.poultry.backend.repository.FarmRepository farmRepository;

    @Autowired
    private com.poultry.backend.repository.FarmMemberRepository farmMemberRepository;

    private User managerUser;
    private User workerUser;
    private String managerToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        farmMemberRepository.deleteAll();
        farmRepository.deleteAll();
        chickenRepository.deleteAll();
        userRepository.deleteAll();

        Farm farm = Farm.builder()
                .name("Registration Test Farm")
                .farmUniqueId("FARM-REG-001")
                .joinCode("REG123")
                .build();
        farm = farmRepository.save(farm);

        // 1. Manager User
        managerUser = User.builder()
                .fullName("Manager Alice")
                .email("manager.alice@example.com")
                .phoneNumber("+15551112222")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        managerUser = userRepository.save(managerUser);
        farmMemberRepository.save(FarmMember.builder()
                .farm(farm)
                .user(managerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build());
        managerToken = jwtUtils.generateToken(new CustomUserDetails(managerUser, "PRIMARY_OWNER"));

        // 2. Worker User
        workerUser = User.builder()
                .fullName("Worker Bob")
                .email("worker.bob@example.com")
                .phoneNumber("+15553334444")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        workerUser = userRepository.save(workerUser);
        farmMemberRepository.save(FarmMember.builder()
                .farm(farm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build());
        workerToken = jwtUtils.generateToken(new CustomUserDetails(workerUser, "WORKER"));
    }

    @Test
    void testGetNextChickenCode_Success() throws Exception {
        mockMvc.perform(get("/api/v1/chickens/next-code")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", startsWith("CHK-")));
    }

    @Test
    void testRegisterChicken_FullFlow_Success() throws Exception {
        ChickenRequest request = ChickenRequest.builder()
                .category(ChickenCategory.COUNTRY_CHICKEN)
                .breed(Breed.RHODE_ISLAND_RED)
                .gender(Gender.FEMALE)
                .color("Reddish Brown")
                .weight(1.85)
                .status(ChickenStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .dateOfBirth(LocalDate.now().minusMonths(6))
                .origin(ChickenOrigin.PURCHASED)
                .purchaseDate(LocalDate.now().minusMonths(3))
                .purchaseCost(25.50)
                .supplierName("Sunrise Hatcheries")
                .supplierContact("+15558889999")
                .wingTagNumber("WT-99")
                .legBandNumber("LB-101")
                .vaccinated(true)
                .vaccinations(List.of(
                        ChickenVaccinationDTO.builder()
                                .vaccineName("Newcastle Disease Vaccine")
                                .vaccinationDate(LocalDate.now().minusMonths(2))
                                .nextDueDate(LocalDate.now().plusMonths(4))
                                .notes("Primary dose administered cleanly")
                                .build()
                ))
                .photoUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .remarks("High yield layer hen")
                .build();

        mockMvc.perform(post("/api/v1/chickens")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.chickenCode", startsWith("CHK-")))
                .andExpect(jsonPath("$.data.category", is("COUNTRY_CHICKEN")))
                .andExpect(jsonPath("$.data.breed", is("RHODE_ISLAND_RED")))
                .andExpect(jsonPath("$.data.gender", is("FEMALE")))
                .andExpect(jsonPath("$.data.wingTagNumber", is("WT-99")))
                .andExpect(jsonPath("$.data.vaccinated", is(true)))
                .andExpect(jsonPath("$.data.vaccinations", hasSize(1)));
    }

    @Test
    void testRegisterChicken_InvalidWeight_Returns400BadRequest() throws Exception {
        ChickenRequest request = ChickenRequest.builder()
                .category(ChickenCategory.BROILER)
                .breed(Breed.COBB_500)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.ACTIVE)
                .weight(-0.5) // Invalid negative weight
                .build();

        mockMvc.perform(post("/api/v1/chickens")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterChicken_SameFatherAndMother_Returns400BadRequest() throws Exception {
        Chicken parent = Chicken.builder()
                .chickenCode("CHK-000088")
                .breed(Breed.SUSSEX)
                .category(ChickenCategory.BREEDER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusYears(1))
                .status(ChickenStatus.ACTIVE)
                .build();
        parent = chickenRepository.save(parent);

        ChickenRequest request = ChickenRequest.builder()
                .category(ChickenCategory.CHICK)
                .breed(Breed.SUSSEX)
                .gender(Gender.UNKNOWN)
                .dateOfBirth(LocalDate.now().minusDays(10))
                .status(ChickenStatus.ACTIVE)
                .fatherId(parent.getId())
                .motherId(parent.getId()) // Invalid same parent
                .build();

        mockMvc.perform(post("/api/v1/chickens")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterChicken_AsWorker_Returns403Forbidden() throws Exception {
        ChickenRequest request = ChickenRequest.builder()
                .category(ChickenCategory.LAYER)
                .breed(Breed.LEGHORN)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(40))
                .status(ChickenStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/chickens")
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
