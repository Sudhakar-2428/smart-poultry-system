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
class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private FarmSettingRepository farmSettingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinanceEventListener financeEventListener;

    private Customer testCustomer;
    private Chicken testChicken;
    private EggBatch testEggBatch;

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
        salesOrderItemRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        eggBatchRepository.deleteAll();
        chickenRepository.deleteAll();
        farmSettingRepository.deleteAll();

        // Save threshold configuration to trigger notification during testing boundaries
        farmSettingRepository.save(FarmSetting.builder()
                .key("large_sale_threshold")
                .value("4000.0")
                .build());

        // Create a test customer
        testCustomer = Customer.builder()
                .customerCode("CUST-007")
                .customerName("Egg & Poultry Dealer Inc")
                .customerType(CustomerType.WHOLESALE)
                .status(CustomerStatus.ACTIVE)
                .build();
        testCustomer = customerRepository.save(testCustomer);

        // Create hens for EggBatch source Hen reference
        Chicken sourceHen = Chicken.builder()
                .chickenCode("HEN-TEST-88")
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.LAYER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(300))
                .status(ChickenStatus.ACTIVE)
                .build();
        sourceHen = chickenRepository.save(sourceHen);

        // Create an active chicken for sales tests
        testChicken = Chicken.builder()
                .chickenCode("CHK-SALE-01")
                .breed(Breed.RHODE_ISLAND_RED)
                .category(ChickenCategory.BROILER)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusDays(120))
                .status(ChickenStatus.ACTIVE)
                .build();
        testChicken = chickenRepository.save(testChicken);

        // Create an egg batch for sales tests
        testEggBatch = EggBatch.builder()
                .batchCode("EG-SALE-01")
                .batchDate(LocalDate.now().minusDays(3))
                .sourceHen(sourceHen)
                .totalEggs(100)
                .goodEggs(80)
                .damagedEggs(5)
                .purpose(EggPurpose.CONSUMPTION)
                .status(EggBatchStatus.CREATED)
                .expectedHatchDate(LocalDate.now().plusDays(18))
                .build();
        testEggBatch = eggBatchRepository.save(testEggBatch);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateCustomer_Success() throws Exception {
        CustomerRequest request = CustomerRequest.builder()
                .customerCode("CUST-NEW-99")
                .customerName("Retailer Farms Group")
                .customerType(CustomerType.RETAIL)
                .status(CustomerStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.customerCode", is("CUST-NEW-99")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateCustomerCode_Rejected() throws Exception {
        CustomerRequest request = CustomerRequest.builder()
                .customerCode("CUST-007") // duplicate
                .customerName("Duplicate Inc")
                .customerType(CustomerType.RETAIL)
                .status(CustomerStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testChickenSale_Success() throws Exception {
        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.CHICKEN)
                .chickenId(testChicken.getId())
                .quantity(1.0)
                .unitPrice(15.0)
                .remarks("Healthy Broiler")
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-CHK-01")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.CHICKEN)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.CONFIRMED)
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(15.0)));

        // Verify chicken status updated to SOLD and sale date logged
        Chicken updatedChicken = chickenRepository.findById(testChicken.getId()).orElseThrow();
        assertEquals(ChickenStatus.SOLD, updatedChicken.getStatus());
        assertEquals(LocalDate.now(), updatedChicken.getSaleDate());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDuplicateChickenSale_Rejected() throws Exception {
        // First sell the chicken
        testChicken.setStatus(ChickenStatus.SOLD);
        testChicken.setSaleDate(LocalDate.now().minusDays(1));
        chickenRepository.save(testChicken);

        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.CHICKEN)
                .chickenId(testChicken.getId())
                .quantity(1.0)
                .unitPrice(15.0)
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-DUP-CHK")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.CHICKEN)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.CONFIRMED)
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("has already been sold")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testEggSale_Success() throws Exception {
        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.EGG_BATCH)
                .eggBatchId(testEggBatch.getId())
                .quantity(30.0)
                .unitPrice(0.2)
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-EGG-01")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.EGG)
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.CONFIRMED)
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal", is(6.0)));

        // Verify egg batch good count reduced
        EggBatch updatedBatch = eggBatchRepository.findById(testEggBatch.getId()).orElseThrow();
        assertEquals(50, updatedBatch.getGoodEggs()); // 80 - 30 = 50
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInsufficientEggs_Rejected() throws Exception {
        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.EGG_BATCH)
                .eggBatchId(testEggBatch.getId())
                .quantity(100.0) // batch has only 80 good eggs
                .unitPrice(0.2)
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-EGG-FAIL")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.EGG)
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.CONFIRMED)
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not have sufficient eggs")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testOrderCancellation_RollbackInventory() throws Exception {
        // 1. Create order as CONFIRMED
        SalesOrderItemRequest itemRequest1 = SalesOrderItemRequest.builder()
                .itemType(ItemType.CHICKEN)
                .chickenId(testChicken.getId())
                .quantity(1.0)
                .unitPrice(20.0)
                .build();

        SalesOrderItemRequest itemRequest2 = SalesOrderItemRequest.builder()
                .itemType(ItemType.EGG_BATCH)
                .eggBatchId(testEggBatch.getId())
                .quantity(10.0)
                .unitPrice(0.5)
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-CANCEL-TEST")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.MIXED)
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PENDING)
                .status(SalesOrderStatus.CONFIRMED)
                .items(List.of(itemRequest1, itemRequest2))
                .build();

        String responseContent = mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(responseContent).get("data").get("id").asLong();

        // Assert inventory is locking
        assertEquals(ChickenStatus.SOLD, chickenRepository.findById(testChicken.getId()).orElseThrow().getStatus());
        assertEquals(70, eggBatchRepository.findById(testEggBatch.getId()).orElseThrow().getGoodEggs());

        // 2. Cancel order
        SalesOrderStatusRequest statusRequest = SalesOrderStatusRequest.builder()
                .status(SalesOrderStatus.CANCELLED)
                .remarks("Cancelled by admin")
                .build();

        mockMvc.perform(patch("/sales-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        // Assert chicken status restored to ACTIVE and Egg Batch restored
        assertEquals(ChickenStatus.ACTIVE, chickenRepository.findById(testChicken.getId()).orElseThrow().getStatus());
        assertNull(chickenRepository.findById(testChicken.getId()).orElseThrow().getSaleDate());
        assertEquals(80, eggBatchRepository.findById(testEggBatch.getId()).orElseThrow().getGoodEggs());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFinanceEventAndNotifications() throws Exception {
        // Prepare large sale order (>= 4000.0)
        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.EGG_BATCH)
                .eggBatchId(testEggBatch.getId())
                .quantity(10.0)
                .unitPrice(450.0) // 10 * 450 = 4500.0
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-LARGE-01")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.EGG)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.COMPLETED) // COMPLETED status triggers financial publish
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify FinanceEvent published
        assertFalse(financeEventListener.getEvents().isEmpty());
        FinanceEvent lastEvent = financeEventListener.getEvents().get(financeEventListener.getEvents().size() - 1);
        assertEquals("SALES_REVENUE", lastEvent.getEventType());
        assertEquals(4500.0, lastEvent.getAmount());

        // Verify Notification (Large Sale is >= 4000)
        List<Notification> notifications = notificationRepository.findAll();
        boolean hasLargeSaleNotification = notifications.stream()
                .anyMatch(n -> n.getType().equals("LARGE_SALE_ALERT"));
        assertTrue(hasLargeSaleNotification, "Should create LARGE_SALE_ALERT notification");
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testAuthorization_WorkerWriteBlocked() throws Exception {
        CustomerRequest request = CustomerRequest.builder()
                .customerCode("CUST-WORKER-BAD")
                .customerName("No Right Buyer")
                .customerType(CustomerType.RETAIL)
                .status(CustomerStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilteringAndPagination() throws Exception {
        // Create an order in draft state
        SalesOrderItemRequest itemRequest = SalesOrderItemRequest.builder()
                .itemType(ItemType.CHICKEN)
                .chickenId(testChicken.getId())
                .quantity(1.0)
                .unitPrice(10.0)
                .build();

        SalesOrderRequest request = SalesOrderRequest.builder()
                .orderNumber("SO-SEARCH-01")
                .customerId(testCustomer.getId())
                .orderDate(LocalDate.now())
                .saleType(SaleType.CHICKEN)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID)
                .status(SalesOrderStatus.DRAFT)
                .items(List.of(itemRequest))
                .build();

        mockMvc.perform(post("/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Search with filters
        mockMvc.perform(get("/sales-orders")
                        .param("customerId", testCustomer.getId().toString())
                        .param("orderStatus", "DRAFT")
                        .param("saleType", "CHICKEN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].orderNumber", is("SO-SEARCH-01")));
    }
}
