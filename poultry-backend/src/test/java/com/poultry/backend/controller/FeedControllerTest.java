package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.common.FinanceEvent;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private FeedSupplierRepository feedSupplierRepository;

    @Autowired
    private FeedPurchaseRepository feedPurchaseRepository;

    @Autowired
    private FeedConsumptionRepository feedConsumptionRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private BrooderBatchRepository brooderBatchRepository;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private IncubatorBatchRepository incubatorBatchRepository;

    @Autowired
    private HatchResultRepository hatchResultRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinanceEventListener financeEventListener;

    private Chicken testChicken;
    private BrooderBatch testBrooder;
    private FeedSupplier testSupplier;
    private FeedItem testFeedItem;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public FinanceEventListener financeEventListener() {
            return new FinanceEventListener();
        }
    }

    static class FinanceEventListener {
        private final List<FinanceEvent> events = Collections.synchronizedList(new ArrayList<>());

        @org.springframework.context.event.EventListener
        public void handleFinanceEvent(FinanceEvent event) {
            events.add(event);
        }

        public List<FinanceEvent> getEvents() {
            return events;
        }

        public void clear() {
            events.clear();
        }
    }

    @BeforeEach
    void setUp() {
        financeEventListener.clear();
        notificationRepository.deleteAll();
        feedConsumptionRepository.deleteAll();
        feedPurchaseRepository.deleteAll();
        feedSupplierRepository.deleteAll();
        feedItemRepository.deleteAll();
        brooderBatchRepository.deleteAll();
        hatchResultRepository.deleteAll();
        incubatorBatchRepository.deleteAll();
        eggBatchRepository.deleteAll();
        chickenRepository.deleteAll();

        // 1. Create supplier
        testSupplier = FeedSupplier.builder()
                .supplierCode("SUP-001")
                .supplierName("Poultry Feed Co.")
                .status(SupplierStatus.ACTIVE)
                .build();
        testSupplier = feedSupplierRepository.save(testSupplier);

        // 2. Create feed item
        testFeedItem = FeedItem.builder()
                .feedCode("FD-STARTER-01")
                .feedName("Starter Mash A")
                .feedType(FeedType.STARTER)
                .unit("KG")
                .minimumStock(10.0)
                .currentStock(50.0)
                .unitCost(1.5)
                .storageLocation("Barn A")
                .status(FeedStatus.ACTIVE)
                .build();
        testFeedItem = feedItemRepository.save(testFeedItem);

        // 3. Create chicken
        testChicken = Chicken.builder()
                .chickenCode("CHK-FEED-01")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(100))
                .status(ChickenStatus.ACTIVE)
                .build();
        testChicken = chickenRepository.save(testChicken);

        // 4. Create brooder batch
        Chicken sourceHen = Chicken.builder()
                .chickenCode("HEN-FEED-99")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(300))
                .status(ChickenStatus.ACTIVE)
                .build();
        chickenRepository.save(sourceHen);

        EggBatch eggBatch = EggBatch.builder()
                .batchCode("EG-FD-01")
                .batchDate(LocalDate.now().minusDays(25))
                .sourceHen(sourceHen)
                .totalEggs(10)
                .goodEggs(10)
                .damagedEggs(0)
                .status(EggBatchStatus.INCUBATING)
                .purpose(EggPurpose.HATCHING)
                .expectedHatchDate(LocalDate.now().minusDays(3))
                .build();
        eggBatch = eggBatchRepository.save(eggBatch);

        IncubatorBatch incubatorBatch = IncubatorBatch.builder()
                .batchCode("INC-FD-01")
                .eggBatch(eggBatch)
                .startDate(LocalDate.now().minusDays(24))
                .expectedHatchDate(LocalDate.now().minusDays(3))
                .status(IncubatorStatus.COMPLETED)
                .build();
        incubatorBatch = incubatorBatchRepository.save(incubatorBatch);

        HatchResult hatch = HatchResult.builder()
                .incubatorBatch(incubatorBatch)
                .totalEggs(10)
                .fertileEggs(10)
                .hatchedChicks(8)
                .deadEmbryos(1)
                .unhatchedEggs(1)
                .hatchPercentage(80.0)
                .recordedDate(LocalDate.now().minusDays(3))
                .build();
        hatch = hatchResultRepository.save(hatch);

        testBrooder = BrooderBatch.builder()
                .brooderCode("BRD-FD-01")
                .hatchResult(hatch)
                .startDate(LocalDate.now().minusDays(3))
                .expectedEndDate(LocalDate.now().plusDays(25))
                .status(BrooderStatus.ACTIVE)
                .build();
        testBrooder = brooderBatchRepository.save(testBrooder);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateFeedItem_Success() throws Exception {
        FeedItemRequest request = FeedItemRequest.builder()
                .feedCode("FD-GROWER-02")
                .feedName("Grower Pellets")
                .feedType(FeedType.GROWER)
                .unit("KG")
                .minimumStock(20.0)
                .currentStock(100.0)
                .unitCost(2.0)
                .storageLocation("Barn B")
                .status(FeedStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/feed-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.feedCode", is("FD-GROWER-02")))
                .andExpect(jsonPath("$.data.currentStock", is(100.0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateFeedCode_Rejected() throws Exception {
        FeedItemRequest request = FeedItemRequest.builder()
                .feedCode("FD-STARTER-01") // duplicate code
                .feedName("Duplicate Feed")
                .feedType(FeedType.STARTER)
                .unit("KG")
                .minimumStock(10.0)
                .currentStock(50.0)
                .unitCost(1.5)
                .storageLocation("Barn A")
                .status(FeedStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/feed-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("is already registered")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSupplier_Success() throws Exception {
        FeedSupplierRequest request = FeedSupplierRequest.builder()
                .supplierCode("SUP-099")
                .supplierName("Alpha Feed Supplies")
                .contactPerson("John Doe")
                .status(SupplierStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/feed-suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.supplierCode", is("SUP-099")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateSupplierCode_Rejected() throws Exception {
        FeedSupplierRequest request = FeedSupplierRequest.builder()
                .supplierCode("SUP-001") // duplicate code
                .supplierName("Dup Supplier")
                .status(SupplierStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/feed-suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRecordPurchase_Success() throws Exception {
        int initialEvents = financeEventListener.getEvents().size();
        
        FeedPurchaseRequest request = FeedPurchaseRequest.builder()
                .purchaseCode("PUR-FD-01")
                .supplierId(testSupplier.getId())
                .purchaseDate(LocalDate.now())
                .feedItemId(testFeedItem.getId())
                .quantity(100.0)
                .unitPrice(1.8)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        mockMvc.perform(post("/feed-purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(180.0)));

        // Verify Automatic Stock Increase
        FeedItem updatedFeed = feedItemRepository.findById(testFeedItem.getId()).orElseThrow();
        assertEquals(150.0, updatedFeed.getCurrentStock());

        // Verify Finance Event Creation
        assertEquals(initialEvents + 1, financeEventListener.getEvents().size());
        FinanceEvent lastEvent = financeEventListener.getEvents().get(financeEventListener.getEvents().size() - 1);
        assertEquals("FEED_PURCHASE_EXPENSE", lastEvent.getEventType());
        assertEquals(180.0, lastEvent.getAmount());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRecordFeedConsumption_Success() throws Exception {
        FeedConsumptionRequest request = FeedConsumptionRequest.builder()
                .feedItemId(testFeedItem.getId())
                .chickenId(testChicken.getId())
                .consumptionDate(LocalDate.now())
                .quantity(15.0)
                .feedingType(FeedingType.INDIVIDUAL)
                .build();

        mockMvc.perform(post("/feed-consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantity", is(15.0)));

        // Verify Stock decrease
        FeedItem updatedFeed = feedItemRepository.findById(testFeedItem.getId()).orElseThrow();
        assertEquals(35.0, updatedFeed.getCurrentStock());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInsufficientStock_Rejected() throws Exception {
        FeedConsumptionRequest request = FeedConsumptionRequest.builder()
                .feedItemId(testFeedItem.getId())
                .chickenId(testChicken.getId())
                .consumptionDate(LocalDate.now())
                .quantity(60.0) // current stock is 50.0
                .feedingType(FeedingType.INDIVIDUAL)
                .build();

        mockMvc.perform(post("/feed-consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExpiredFeedAllocation_Rejected() throws Exception {
        // Expiration of feed item
        testFeedItem.setExpiryDate(LocalDate.now().minusDays(2));
        testFeedItem.setStatus(FeedStatus.EXPIRED);
        feedItemRepository.save(testFeedItem);

        FeedConsumptionRequest request = FeedConsumptionRequest.builder()
                .feedItemId(testFeedItem.getId())
                .chickenId(testChicken.getId())
                .consumptionDate(LocalDate.now())
                .quantity(5.0)
                .feedingType(FeedingType.INDIVIDUAL)
                .build();

        mockMvc.perform(post("/feed-consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot allocate expired")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testLowStockNotification() throws Exception {
        // Drop stock below minimumStock (10.0 KG)
        FeedConsumptionRequest request = FeedConsumptionRequest.builder()
                .feedItemId(testFeedItem.getId())
                .chickenId(testChicken.getId())
                .consumptionDate(LocalDate.now())
                .quantity(45.0) // 50.0 - 45.0 = 5.0 (below 10.0)
                .feedingType(FeedingType.INDIVIDUAL)
                .build();

        mockMvc.perform(post("/feed-consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<Notification> alerts = notificationRepository.findByType("LOW_STOCK_WARNING");
        assertFalse(alerts.isEmpty());
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testAuthorization_WorkerReadAllowedWriteBlocked() throws Exception {
        FeedItemRequest request = FeedItemRequest.builder()
                .feedCode("FD-WORKER-TEST")
                .feedName("Worker Feed")
                .feedType(FeedType.OTHER)
                .unit("KG")
                .minimumStock(5.0)
                .currentStock(10.0)
                .unitCost(1.0)
                .storageLocation("Barn C")
                .status(FeedStatus.ACTIVE)
                .build();

        // Write is BLOCKED
        mockMvc.perform(post("/feed-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Read is ALLOWED
        mockMvc.perform(get("/feed-items"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilteringAndPagination() throws Exception {
        mockMvc.perform(get("/feed-items")
                        .param("feedType", "STARTER")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].feedCode", is("FD-STARTER-01")));
    }
}
