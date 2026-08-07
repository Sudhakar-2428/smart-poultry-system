package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.PurchaseBatchDTOs;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenOrigin;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import com.poultry.backend.repository.PurchaseBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PurchaseBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PurchaseBatchRepository purchaseBatchRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private ChickenTimelineRepository chickenTimelineRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePurchaseBatch_AutoSequence_PB01() throws Exception {
        PurchaseBatchDTOs.CreatePurchaseBatchRequest req1 = PurchaseBatchDTOs.CreatePurchaseBatchRequest.builder()
                .supplierName("Apex Poultry Farm")
                .supplierContact("+91 98765 43210")
                .purchaseDate(LocalDate.now())
                .purchaseCost(BigDecimal.valueOf(1500))
                .totalChickensCount(3)
                .category(ChickenCategory.COUNTRY_CHICKEN)
                .breed(Breed.COUNTRY_CHICKEN)
                .gender(Gender.FEMALE)
                .build();

        mockMvc.perform(post("/purchase-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchCode", is("PB01")))
                .andExpect(jsonPath("$.data.totalChickensCount", is(3)))
                .andExpect(jsonPath("$.data.registeredChickens[0].chickenCode", is("PB01-001")))
                .andExpect(jsonPath("$.data.registeredChickens[1].chickenCode", is("PB01-002")))
                .andExpect(jsonPath("$.data.registeredChickens[2].chickenCode", is("PB01-003")));

        // Verify second batch resets sequence to 001 under PB02
        PurchaseBatchDTOs.CreatePurchaseBatchRequest req2 = PurchaseBatchDTOs.CreatePurchaseBatchRequest.builder()
                .supplierName("Royal Hatchery")
                .supplierContact("+91 91234 56789")
                .purchaseDate(LocalDate.now())
                .purchaseCost(BigDecimal.valueOf(2000))
                .totalChickensCount(2)
                .category(ChickenCategory.LAYER)
                .breed(Breed.RHODE_ISLAND_RED)
                .gender(Gender.FEMALE)
                .build();

        mockMvc.perform(post("/purchase-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchCode", is("PB02")))
                .andExpect(jsonPath("$.data.totalChickensCount", is(2)))
                .andExpect(jsonPath("$.data.registeredChickens[0].chickenCode", is("PB02-001")))
                .andExpect(jsonPath("$.data.registeredChickens[1].chickenCode", is("PB02-002")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetPurchasedChickenReport() throws Exception {
        mockMvc.perform(get("/purchase-batches/reports")
                        .param("reportType", "SUPPLIER")
                        .param("supplierName", "Apex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
