package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.Chicken;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.repository.ChickenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChickenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        chickenRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateChicken_Success() throws Exception {
        ChickenRequest request = ChickenRequest.builder()
                .chickenCode("CHK-001")
                .name("Clucky")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(20))
                .weight(1.5)
                .color("White")
                .status(ChickenStatus.ACTIVE)
                .photoUrl("http://example.com/photo.jpg")
                .remarks("First flock")
                .build();

        mockMvc.perform(post("/chickens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.chickenCode", is("CHK-001")))
                .andExpect(jsonPath("$.data.ageInDays", is(20)))
                .andExpect(jsonPath("$.data.ageInMonths", is(0))); // 20 days is 0.65 -> truncated in ChronoUnit.MONTHS.between to 0
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateChicken_DuplicateChickenCode() throws Exception {
        Chicken c = Chicken.builder()
                .chickenCode("DUB-100")
                .breed(Breed.HUBBARD)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(10))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(c);

        ChickenRequest request = ChickenRequest.builder()
                .chickenCode("DUB-100") // Duplicate chickenCode
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.BREEDER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(5))
                .status(ChickenStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/chickens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateChicken_FutureDOB_Invalid() throws Exception {
        ChickenRequest request = ChickenRequest.builder()
                .chickenCode("FUTURE-1")
                .breed(Breed.ROSS_308)
                .category(ChickenCategory.BROILER)
                .gender(Gender.UNKNOWN)
                .dateOfBirth(LocalDate.now().plusDays(2)) // Future DOB
                .status(ChickenStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/chickens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testSearchChickens_Filters_Success() throws Exception {
        // Save different chickens
        Chicken c1 = Chicken.builder()
                .chickenCode("A-100")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(100))
                .weight(2.5)
                .status(ChickenStatus.ACTIVE)
                .build();

        Chicken c2 = Chicken.builder()
                .chickenCode("B-200")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(20))
                .weight(1.2)
                .status(ChickenStatus.SOLD)
                .build();

        chickenRepository.save(c1);
        chickenRepository.save(c2);

        // 1. Filter by category
        mockMvc.perform(get("/chickens")
                        .param("category", "LAYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("B-200")));

        // 2. Filter by age limit: max age 50 days (only c2 which has 20 days age matches)
        mockMvc.perform(get("/chickens")
                        .param("maxAgeDays", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("B-200")));

        // 3. Filter by weight range
        mockMvc.perform(get("/chickens")
                        .param("minWeight", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("A-100")));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testSearchChickens_PaginationAndSorting() throws Exception {
        // Create 3 chickens
        for (int i = 1; i <= 3; i++) {
            Chicken c = Chicken.builder()
                    .chickenCode("C-" + i)
                    .breed(Breed.OTHER)
                    .category(ChickenCategory.OTHER)
                    .gender(Gender.UNKNOWN)
                    .dateOfBirth(LocalDate.now().minusDays(i))
                    .status(ChickenStatus.ACTIVE)
                    .build();
            chickenRepository.save(c);
        }

        // Fetch page=0, size=2 sorted by chickenCode desc
        mockMvc.perform(get("/chickens")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "chickenCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].chickenCode", is("C-3")))
                .andExpect(jsonPath("$.data.content[1].chickenCode", is("C-2")))
                .andExpect(jsonPath("$.data.totalPages", is(2)))
                .andExpect(jsonPath("$.data.totalElements", is(3)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateChicken_Success() throws Exception {
        Chicken chicken = Chicken.builder()
                .chickenCode("UPD-001")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(50))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(chicken);

        ChickenRequest request = ChickenRequest.builder()
                .chickenCode("UPD-001")
                .name("Updated Name")
                .breed(Breed.ROSS_308)
                .category(ChickenCategory.LAYER)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusDays(50))
                .status(ChickenStatus.BROODER)
                .weight(2.0)
                .build();

        mockMvc.perform(put("/chickens/" + chicken.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Name")))
                .andExpect(jsonPath("$.data.breed", is("ROSS_308")))
                .andExpect(jsonPath("$.data.category", is("LAYER")))
                .andExpect(jsonPath("$.data.status", is("BROODER")))
                .andExpect(jsonPath("$.data.weight", is(2.0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteChicken_SuccessAndConstraintViolations() throws Exception {
        // Case 1: Delete ACTIVE chicken (Success)
        Chicken activeChicken = Chicken.builder()
                .chickenCode("CHK-DEL-1")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(activeChicken);

        mockMvc.perform(delete("/chickens/" + activeChicken.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        assertFalse(chickenRepository.existsById(activeChicken.getId()));

        // Case 2: Delete SOLD chicken (Forbidden by Business Rule)
        Chicken soldChicken = Chicken.builder()
                .chickenCode("CHK-DEL-2")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.SOLD)
                .build();
        chickenRepository.save(soldChicken);

        mockMvc.perform(delete("/chickens/" + soldChicken.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot delete SOLD chickens")));

        // Case 3: Delete DEAD chicken (Forbidden by Business Rule)
        Chicken deadChicken = Chicken.builder()
                .chickenCode("CHK-DEL-3")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.DEAD)
                .build();
        chickenRepository.save(deadChicken);

        mockMvc.perform(delete("/chickens/" + deadChicken.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Cannot delete DEAD chickens")));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Unauthenticated lookup should return 401
        mockMvc.perform(get("/chickens"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testForbiddenAccessForWorker() throws Exception {
        // WORKER attempts write modifications
        ChickenRequest request = ChickenRequest.builder()
                .chickenCode("WK-1")
                .breed(Breed.COBB_500)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(30))
                .status(ChickenStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/chickens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
