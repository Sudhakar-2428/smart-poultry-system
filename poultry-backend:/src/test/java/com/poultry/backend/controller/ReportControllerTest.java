package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChickenRepository chickenRepository;

    @Autowired
    private EggRecordRepository eggRecordRepository;

    @Autowired
    private EggBatchRepository eggBatchRepository;

    @Autowired
    private IncubatorBatchRepository incubatorBatchRepository;

    @Autowired
    private HatchResultRepository hatchResultRepository;

    @Autowired
    private ChickGrowthRecordRepository chickGrowthRecordRepository;

    @Autowired
    private BreedingPairRepository breedingPairRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private LedgerTransactionRepository transactionRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private FeedConsumptionRepository feedConsumptionRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @BeforeEach
    void setUp() {
        hatchResultRepository.deleteAll();
        incubatorBatchRepository.deleteAll();
        eggBatchRepository.deleteAll();
        eggRecordRepository.deleteAll();
        chickGrowthRecordRepository.deleteAll();
        breedingPairRepository.deleteAll();
        healthRecordRepository.deleteAll();
        feedConsumptionRepository.deleteAll();
        feedItemRepository.deleteAll();
        transactionRepository.deleteAll();
        ledgerAccountRepository.deleteAll();
        salesOrderRepository.deleteAll();

        chickenRepository.deleteAll();

        // 1. Create a Male Chicken
        Chicken rooster = chickenRepository.save(Chicken.builder()
                .chickenCode("CHK-M-01")
                .name("Big Rooster")
                .dateOfBirth(LocalDate.now().minusDays(180))
                .breed(Breed.LEGHORN)
                .category(ChickenCategory.ROOSTER)
                .status(ChickenStatus.ACTIVE)
                .gender(Gender.MALE)
                .weight(3.2)
                .build());

        // 2. Create a Female Laying Hen
        Chicken hen = chickenRepository.save(Chicken.builder()
                .chickenCode("CHK-F-01")
                .name("Laying Hen")
                .dateOfBirth(LocalDate.now().minusDays(120))
                .breed(Breed.RHODE_ISLAND_RED)
                .category(ChickenCategory.LAYER)
                .status(ChickenStatus.ACTIVE)
                .gender(Gender.FEMALE)
                .weight(2.5)
                .build());

        // 3. Create active brooder chick
        chickenRepository.save(Chicken.builder()
                .chickenCode("CHK-C-01")
                .name("Brooder Chick")
                .dateOfBirth(LocalDate.now().minusDays(10))
                .breed(Breed.PLYMOUTH_ROCK)
                .category(ChickenCategory.CHICK)
                .status(ChickenStatus.BROODER)
                .gender(Gender.UNKNOWN)
                .weight(0.15)
                .build());

        // 4. Create Breeding Pair
        breedingPairRepository.save(BreedingPair.builder()
                .pairCode("PR-01")
                .maleChicken(rooster)
                .femaleChicken(hen)
                .startDate(LocalDate.now().minusDays(10))
                .status(PairStatus.ACTIVE)
                .breedingPurpose(BreedingPurpose.NATURAL_BREEDING)
                .expectedEggProduction(20)
                .build());

        // 5. Create Egg Record
        eggRecordRepository.save(EggRecord.builder()
                .hen(hen)
                .numberOfEggs(5)
                .damagedEggs(1)
                .recordDate(LocalDate.now())
                .remarks("Healthy egg run")
                .build());

        // 6. Create Egg Batch
        EggBatch batch = eggBatchRepository.save(EggBatch.builder()
                .batchCode("BAT-01")
                .sourceHen(hen)
                .totalEggs(10)
                .goodEggs(8)
                .damagedEggs(2)
                .batchDate(LocalDate.now())
                .purpose(EggPurpose.HATCHING)
                .status(EggBatchStatus.CREATED)
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .build());

        // 7. Create Incubator Batch
        IncubatorBatch inc = incubatorBatchRepository.save(IncubatorBatch.builder()
                .batchCode("INC-01")
                .eggBatch(batch)
                .startDate(LocalDate.now().minusDays(21))
                .expectedHatchDate(LocalDate.now())
                .status(IncubatorStatus.COMPLETED)
                .build());

        // 8. Create Hatch Result
        hatchResultRepository.save(HatchResult.builder()
                .incubatorBatch(inc)
                .totalEggs(10)
                .fertileEggs(9)
                .hatchedChicks(8)
                .deadEmbryos(1)
                .unhatchedEggs(1)
                .recordedDate(LocalDate.now())
                .hatchPercentage(80.0)
                .build());

        // 9. Create Growth Record for the chick
        Chicken chick = chickenRepository.findAll().stream().filter(c -> c.getStatus() == ChickenStatus.BROODER).findFirst().orElseThrow();
        chickGrowthRecordRepository.save(ChickGrowthRecord.builder()
                .chicken(chick)
                .growthDate(LocalDate.now())
                .ageInDays(10)
                .weight(0.18)
                .height(5.5)
                .healthStatus(HealthStatus.HEALTHY)
                .growthStage(GrowthStage.STARTER)
                .gender(Gender.UNKNOWN)
                .build());

        // 10. Create Health Record
        healthRecordRepository.save(HealthRecord.builder()
                .recordCode("HL-01")
                .chicken(hen)
                .recordDate(LocalDate.now())
                .healthType(HealthType.VACCINATION)
                .vaccinationName("Newcastle Vaccine")
                .nextVaccinationDate(LocalDate.now().plusDays(30))
                .veterinarian("Dr. Smith")
                .healthStatus(HealthStatus.HEALTHY)
                .build());

        // Create ledger account
        LedgerAccount account = ledgerAccountRepository.save(LedgerAccount.builder()
                .accountCode("ACT-01")
                .accountName("Cash Account")
                .accountType(AccountType.CASH)
                .openingBalance(1000.0)
                .currentBalance(1000.0)
                .status(Status.ACTIVE)
                .build());

        // 11. Create Ledger Transaction (Income and Expense)
        transactionRepository.save(LedgerTransaction.builder()
                .transactionCode("TX-INC-01")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.INCOME)
                .amount(500.0)
                .ledgerAccount(account)
                .paymentMethod(PaymentMethod.CASH)
                .referenceType(ReferenceType.MANUAL)
                .build());
        transactionRepository.save(LedgerTransaction.builder()
                .transactionCode("TX-EXP-01")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.EXPENSE)
                .amount(200.0)
                .ledgerAccount(account)
                .paymentMethod(PaymentMethod.CASH)
                .referenceType(ReferenceType.MANUAL)
                .build());


        // 12. Create Feed Item
        FeedItem feed = feedItemRepository.save(FeedItem.builder()
                .feedCode("FE-01")
                .feedName("Starter Mash")
                .feedType(FeedType.STARTER)
                .currentStock(100.0)
                .minimumStock(10.0)
                .unit("kg")
                .status(FeedStatus.ACTIVE)
                .storageLocation("Barn A")
                .build());

        // 13. Create Feed Consumption today
        feedConsumptionRepository.save(FeedConsumption.builder()
                .feedItem(feed)
                .chicken(hen)
                .quantity(1.5)
                .consumptionDate(LocalDate.now())
                .feedingType(FeedingType.INDIVIDUAL)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDashboardSummary_Success() throws Exception {
        mockMvc.perform(get("/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalChickens", is(3)))
                .andExpect(jsonPath("$.data.activeChickens", is(2)))
                .andExpect(jsonPath("$.data.deadChickens", is(0)))
                .andExpect(jsonPath("$.data.currentBrooderChicks", is(1)))
                .andExpect(jsonPath("$.data.totalEggsProduced", is(5)))
                .andExpect(jsonPath("$.data.eggsAvailable", is(8)))
                .andExpect(jsonPath("$.data.currentFeedStock", is(100.0)))
                .andExpect(jsonPath("$.data.todayFeedConsumption", is(1.5)))
                .andExpect(jsonPath("$.data.upcomingVaccinations", is(1)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetChickenReport_Success() throws Exception {
        mockMvc.perform(get("/reports/chickens?dateFilter=custom&startDate=2000-01-01&endDate=2050-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalChickens", is(3)))
                .andExpect(jsonPath("$.data.activeCount", is(2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetEggReport_Success() throws Exception {
        mockMvc.perform(get("/reports/eggs?dateFilter=today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEggsProduced", is(5)))
                .andExpect(jsonPath("$.data.totalDamagedEggs", is(1)))
                .andExpect(jsonPath("$.data.damagedRate", is(20.0)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetHatchingReport_Success() throws Exception {
        mockMvc.perform(get("/reports/hatching?dateFilter=this_month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalIncubated", is(10)))
                .andExpect(jsonPath("$.data.totalHatched", is(8)))
                .andExpect(jsonPath("$.data.hatchRate", is(80.0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetGrowthReport_Success() throws Exception {
        mockMvc.perform(get("/reports/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.growthRecordsCount", is(1)))
                .andExpect(jsonPath("$.data.averageWeight", is(0.18)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetBreedingReport_Success() throws Exception {
        mockMvc.perform(get("/reports/breeding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalBreedingPairs", is(1)))
                .andExpect(jsonPath("$.data.activeBreedingPairs", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetHealthReport_Success() throws Exception {
        mockMvc.perform(get("/reports/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalVaccinations", is(1)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetFeedReport_Success() throws Exception {
        mockMvc.perform(get("/reports/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.currentFeedStock", is(100.0)))
                .andExpect(jsonPath("$.data.totalConsumedToday", is(1.5)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSalesReport_Success() throws Exception {
        mockMvc.perform(get("/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetFinanceReport_Success() throws Exception {
        mockMvc.perform(get("/reports/finance?dateFilter=today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalIncome", is(500.0)))
                .andExpect(jsonPath("$.data.totalExpense", is(200.0)))
                .andExpect(jsonPath("$.data.netProfit", is(300.0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProfitabilityReport_Success() throws Exception {
        mockMvc.perform(get("/reports/profitability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testGetDashboardSummary_WorkerRole_Forbidden() throws Exception {
        mockMvc.perform(get("/reports/dashboard"))
                .andExpect(status().isForbidden());
    }
}
