package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ChickenRepository chickenRepository;
    private final EggRecordRepository eggRecordRepository;
    private final EggBatchRepository eggBatchRepository;
    private final IncubatorBatchRepository incubatorBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final ChickGrowthRecordRepository chickGrowthRecordRepository;
    private final BreedingPairRepository breedingPairRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final FeedItemRepository feedItemRepository;
    private final FeedPurchaseRepository feedPurchaseRepository;
    private final FeedConsumptionRepository feedConsumptionRepository;

    private final SalesService salesService;
    private final FinanceService financeService;
    private final HealthRecordService healthRecordService;
    private final FeedService feedService;

    // Date Range parsing helper
    private DateRange parseDateFilter(String filter, LocalDate customStart, LocalDate customEnd) {
        LocalDate today = LocalDate.now();
        if (filter == null || filter.trim().isEmpty()) {
            return new DateRange(today.minusDays(30), today);
        }
        switch (filter.trim().toLowerCase()) {
            case "today":
                return new DateRange(today, today);
            case "yesterday":
                return new DateRange(today.minusDays(1), today.minusDays(1));
            case "last_7_days":
            case "last 7 days":
                return new DateRange(today.minusDays(7), today);
            case "last_30_days":
            case "last 30 days":
                return new DateRange(today.minusDays(30), today);
            case "this_month":
            case "this month":
                return new DateRange(today.withDayOfMonth(1), today);
            case "last_month":
            case "last month":
                LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastOfLastMonth = today.withDayOfMonth(1).minusDays(1);
                return new DateRange(firstOfLastMonth, lastOfLastMonth);
            case "custom":
            case "custom_date_range":
            case "custom date range":
                LocalDate start = customStart != null ? customStart : today.minusDays(30);
                LocalDate end = customEnd != null ? customEnd : today;
                return new DateRange(start, end);
            default:
                return new DateRange(today.minusDays(30), today);
        }
    }

    private static class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        public DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() { return start; }
        public LocalDate getEnd() { return end; }
    }

    @Override
    @Cacheable(value = "reports", key = "'dashboard'")
    public DashboardSummaryResponse getDashboardSummary() {
        log.info("AUDIT: Dashboard Generated");

        long totalChickens = chickenRepository.count();
        long activeChickens = chickenRepository.countByStatus(ChickenStatus.ACTIVE);
        long deadChickens = chickenRepository.countByStatus(ChickenStatus.DEAD);
        long soldChickens = chickenRepository.countByStatus(ChickenStatus.SOLD);
        long currentBrooderChicks = chickenRepository.countByStatus(ChickenStatus.BROODER);

        long totalEggsProduced = eggRecordRepository.sumTotalEggsProduced();
        long eggsSold = eggBatchRepository.sumEggsSold();
        long eggsAvailable = eggBatchRepository.sumEggsAvailable();

        double currentFeedStock = feedItemRepository.sumCurrentStock();
        List<FeedItemResponse> lowStockItems = feedService.getLowStockItems();
        double todayFeedConsumption = feedConsumptionRepository.sumConsumptionByDate(LocalDate.now());

        double todaySales = salesService.getDailySales(LocalDate.now());
        double monthlyRevenue = salesService.getMonthlySales(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        
        Double monthlyExpensesVal = financeService.getMonthlyExpense(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        double monthlyExpenses = monthlyExpensesVal != null ? monthlyExpensesVal : 0.0;

        Double netProfitVal = financeService.getNetProfit();
        double netProfit = netProfitVal != null ? netProfitVal : 0.0;

        double pendingPayments = salesOrderRepository.sumPendingPayments();
        long upcomingVaccinations = healthRecordRepository.countUpcomingVaccinations(LocalDate.now());
        long criticalHealthCases = healthRecordRepository.countCriticalCases();

        return DashboardSummaryResponse.builder()
                .totalChickens(totalChickens)
                .activeChickens(activeChickens)
                .deadChickens(deadChickens)
                .soldChickens(soldChickens)
                .currentBrooderChicks(currentBrooderChicks)
                .totalEggsProduced(totalEggsProduced)
                .eggsSold(eggsSold)
                .eggsAvailable(eggsAvailable)
                .currentFeedStock(currentFeedStock)
                .lowStockItems(lowStockItems)
                .todayFeedConsumption(todayFeedConsumption)
                .todaySales(todaySales)
                .monthlyRevenue(monthlyRevenue)
                .monthlyExpenses(monthlyExpenses)
                .netProfit(netProfit)
                .pendingPayments(pendingPayments)
                .upcomingVaccinations(upcomingVaccinations)
                .criticalHealthCases(criticalHealthCases)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'chickens_' + #dateFilter")
    public ChickenReportResponse getChickenReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        List<Chicken> chickensList = chickenRepository.findAll().stream()
                .filter(c -> c.getDateOfBirth() != null && !c.getDateOfBirth().isBefore(start) && !c.getDateOfBirth().isAfter(end))
                .toList();

        long total = chickensList.size();
        long active = chickensList.stream().filter(c -> c.getStatus() == ChickenStatus.ACTIVE).count();
        long dead = chickensList.stream().filter(c -> c.getStatus() == ChickenStatus.DEAD).count();
        long sold = chickensList.stream().filter(c -> c.getStatus() == ChickenStatus.SOLD).count();

        double avgWeight = chickensList.stream()
                .filter(c -> c.getWeight() != null)
                .mapToDouble(Chicken::getWeight)
                .average()
                .orElse(0.0);

        double avgAge = chickensList.stream()
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getDateOfBirth(), LocalDate.now()))
                .average()
                .orElse(0.0);

        double mortalityRate = total > 0 ? ((double) dead / total) * 100.0 : 0.0;

        Map<String, Long> breedDist = chickensList.stream()
                .filter(c -> c.getBreed() != null)
                .collect(Collectors.groupingBy(c -> c.getBreed().name(), Collectors.counting()));

        Map<String, Long> genderDist = chickensList.stream()
                .filter(c -> c.getGender() != null)
                .collect(Collectors.groupingBy(c -> c.getGender().name(), Collectors.counting()));

        Map<String, Long> catDist = chickensList.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(c -> c.getCategory().name(), Collectors.counting()));

        Map<String, Long> statusDist = chickensList.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));

        return ChickenReportResponse.builder()
                .totalChickens(total)
                .activeCount(active)
                .deadCount(dead)
                .soldCount(sold)
                .averageWeight(avgWeight)
                .averageAgeDays(avgAge)
                .mortalityRate(mortalityRate)
                .breedDistribution(breedDist)
                .genderDistribution(genderDist)
                .categoryDistribution(catDist)
                .statusDistribution(statusDist)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'eggs_' + #dateFilter")
    public EggReportResponse getEggReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        long produced = eggRecordRepository.sumTotalEggsProducedInRange(start, end);
        long damaged = eggRecordRepository.sumDamagedEggsInRange(start, end);
        double damagedRate = produced > 0 ? ((double) damaged / produced) * 100.0 : 0.0;

        long sold = eggBatchRepository.sumEggsSoldInRange(start, end);
        long available = eggBatchRepository.sumEggsAvailable();

        long daysCount = Math.max(1L, ChronoUnit.DAYS.between(start, end) + 1);
        double dailyAvg = (double) produced / daysCount;

        // Fetch batches in date range for purpose/status distributions
        List<EggBatch> batches = eggBatchRepository.findAll().stream()
                .filter(b -> b.getBatchDate() != null && !b.getBatchDate().isBefore(start) && !b.getBatchDate().isAfter(end))
                .toList();

        Map<String, Long> purposeDist = batches.stream()
                .filter(b -> b.getPurpose() != null)
                .collect(Collectors.groupingBy(b -> b.getPurpose().name(), Collectors.counting()));

        Map<String, Long> statusDist = batches.stream()
                .filter(b -> b.getStatus() != null)
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        // Daily production trend
        List<EggRecord> records = eggRecordRepository.findAll().stream()
                .filter(r -> r.getRecordDate() != null && !r.getRecordDate().isBefore(start) && !r.getRecordDate().isAfter(end))
                .toList();

        Map<LocalDate, Long> productionTrend = records.stream()
                .collect(Collectors.groupingBy(
                        EggRecord::getRecordDate,
                        Collectors.summingLong(EggRecord::getNumberOfEggs)
                ));

        return EggReportResponse.builder()
                .totalEggsProduced(produced)
                .totalDamagedEggs(damaged)
                .damagedRate(damagedRate)
                .eggsSold(sold)
                .eggsAvailable(available)
                .dailyAverage(dailyAvg)
                .purposeDistribution(purposeDist)
                .statusDistribution(statusDist)
                .productionTrend(productionTrend)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'hatching_' + #dateFilter")
    public HatchingReportResponse getHatchingReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        long totalIncubated = hatchResultRepository.sumTotalEggsInRange(start, end);
        long totalHatched = hatchResultRepository.sumHatchedChicksInRange(start, end);
        long totalFailed = hatchResultRepository.sumFailedEggsInRange(start, end);
        double hatchRate = totalIncubated > 0 ? ((double) totalHatched / totalIncubated) * 100.0 : 0.0;

        List<HatchResult> results = hatchResultRepository.findAll().stream()
                .filter(h -> h.getRecordedDate() != null && !h.getRecordedDate().isBefore(start) && !h.getRecordedDate().isAfter(end))
                .toList();

        double avgIncubationVal = results.stream()
                .filter(h -> h.getIncubatorBatch() != null && h.getIncubatorBatch().getStartDate() != null)
                .mapToLong(h -> ChronoUnit.DAYS.between(h.getIncubatorBatch().getStartDate(), h.getRecordedDate()))
                .average()
                .orElse(21.0); // 21 days standard gestation if average calculations find empty source rows

        long activeCount = incubatorBatchRepository.countActiveBatches();
        long completedCount = incubatorBatchRepository.countCompletedBatches();

        Map<String, Double> breedHatchRate = results.stream()
                .filter(h -> h.getIncubatorBatch() != null && h.getIncubatorBatch().getEggBatch() != null && h.getIncubatorBatch().getEggBatch().getSourceHen() != null)
                .collect(Collectors.groupingBy(
                        h -> h.getIncubatorBatch().getEggBatch().getSourceHen().getBreed().name(),
                        Collectors.averagingDouble(HatchResult::getHatchPercentage)
                ));

        return HatchingReportResponse.builder()
                .totalIncubated(totalIncubated)
                .totalHatched(totalHatched)
                .totalFailed(totalFailed)
                .hatchRate(hatchRate)
                .averageIncubationDays(avgIncubationVal)
                .activeIncubatorBatches(activeCount)
                .completedIncubatorBatches(completedCount)
                .hatchRateByBreed(breedHatchRate)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'growth_' + #dateFilter")
    public GrowthReportResponse getGrowthReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        Double avgW = chickGrowthRecordRepository.getAverageWeightInRange(start, end);
        double avgWeight = avgW != null ? avgW : 0.0;

        Double avgH = chickGrowthRecordRepository.getAverageHeightInRange(start, end);
        double avgHeight = avgH != null ? avgH : 0.0;

        long recordsCount = chickGrowthRecordRepository.countByGrowthDateInRange(start, end);

        List<ChickGrowthRecord> recordsList = chickGrowthRecordRepository.findAll().stream()
                .filter(r -> r.getGrowthDate() != null && !r.getGrowthDate().isBefore(start) && !r.getGrowthDate().isAfter(end))
                .toList();

        double avgWeightGain = recordsList.stream()
                .mapToDouble(r -> r.getAgeInDays() > 0 ? r.getWeight() / r.getAgeInDays() : r.getWeight())
                .average()
                .orElse(0.0);

        Map<String, Long> stageDist = recordsList.stream()
                .filter(r -> r.getGrowthStage() != null)
                .collect(Collectors.groupingBy(r -> r.getGrowthStage().name(), Collectors.counting()));

        Map<String, Long> healthDist = recordsList.stream()
                .filter(r -> r.getHealthStatus() != null)
                .collect(Collectors.groupingBy(r -> r.getHealthStatus().name(), Collectors.counting()));

        return GrowthReportResponse.builder()
                .averageWeightGain(avgWeightGain)
                .averageWeight(avgWeight)
                .averageHeight(avgHeight)
                .growthRecordsCount(recordsCount)
                .growthStageDistribution(stageDist)
                .healthStatusDistribution(healthDist)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'breeding_' + #dateFilter")
    public BreedingReportResponse getBreedingReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        long total = breedingPairRepository.countBreedingPairsInRange(start, end);
        long active = breedingPairRepository.countActiveBreedingPairsInRange(start, end);
        long expectedEggs = breedingPairRepository.sumExpectedEggProductionInRange(start, end);

        List<BreedingPair> pairsList = breedingPairRepository.findAll().stream()
                .filter(b -> b.getStartDate() != null && !b.getStartDate().isBefore(start) && !b.getStartDate().isAfter(end))
                .toList();

        Map<String, Long> purposeDist = pairsList.stream()
                .filter(b -> b.getBreedingPurpose() != null)
                .collect(Collectors.groupingBy(b -> b.getBreedingPurpose().name(), Collectors.counting()));

        Map<String, Long> statusDist = pairsList.stream()
                .filter(b -> b.getStatus() != null)
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        return BreedingReportResponse.builder()
                .totalBreedingPairs(total)
                .activeBreedingPairs(active)
                .totalExpectedEggProduction(expectedEggs)
                .breedingPurposeDistribution(purposeDist)
                .statusDistribution(statusDist)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'health_' + #dateFilter")
    public HealthAnalyticsResponse getHealthReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        log.info("AUDIT: Health Report Viewed");
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        List<HealthRecord> records = healthRecordRepository.findAll().stream()
                .filter(h -> h.getRecordDate() != null && !h.getRecordDate().isBefore(start) && !h.getRecordDate().isAfter(end))
                .toList();

        long totalVaccinations = records.stream().filter(h -> h.getHealthType() == HealthType.VACCINATION).count();
        long diseaseCount = records.stream().filter(h -> h.getHealthType() == HealthType.DISEASE).count();
        long mortalityCount = records.stream().filter(h -> h.getMortality() != null && h.getMortality()).count();
        long recoveryCount = records.stream().filter(h -> h.getHealthStatus() == HealthStatus.RECOVERING).count();
        long treatmentCount = records.stream().filter(h -> h.getHealthStatus() == HealthStatus.UNDER_TREATMENT).count();
        double compliance = healthRecordService.getVaccinationCompliance();

        Map<String, Long> diseaseDist = records.stream()
                .filter(h -> h.getDiseaseName() != null && !h.getDiseaseName().trim().isEmpty())
                .collect(Collectors.groupingBy(HealthRecord::getDiseaseName, Collectors.counting()));

        Map<String, Long> complianceByBatch = records.stream()
                .filter(h -> h.getVaccinationBatch() != null && !h.getVaccinationBatch().trim().isEmpty())
                .collect(Collectors.groupingBy(HealthRecord::getVaccinationBatch, Collectors.counting()));

        return HealthAnalyticsResponse.builder()
                .totalVaccinations(totalVaccinations)
                .diseaseCount(diseaseCount)
                .mortalityCount(mortalityCount)
                .recoveryCount(recoveryCount)
                .treatmentCount(treatmentCount)
                .vaccinationCompliance(compliance)
                .diseaseDistributions(diseaseDist)
                .vaccineComplianceByBatch(complianceByBatch)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'feed_' + #dateFilter")
    public FeedAnalyticsResponse getFeedReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        log.info("AUDIT: Feed Report Viewed");
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        double stock = feedItemRepository.sumCurrentStock();
        int lowStockCount = feedService.getLowStockItems().size();
        double dailyConsumed = feedConsumptionRepository.sumConsumptionByDate(LocalDate.now());

        double purchaseCost = feedPurchaseRepository.findAll().stream()
                .filter(p -> p.getPurchaseDate() != null && !p.getPurchaseDate().isBefore(start) && !p.getPurchaseDate().isAfter(end))
                .mapToDouble(FeedPurchase::getTotalAmount)
                .sum();

        List<FeedConsumption> consumptionList = feedConsumptionRepository.findAll().stream()
                .filter(c -> c.getConsumptionDate() != null && !c.getConsumptionDate().isBefore(start) && !c.getConsumptionDate().isAfter(end))
                .toList();

        Map<String, Double> usageByType = consumptionList.stream()
                .filter(c -> c.getFeedItem() != null && c.getFeedItem().getFeedType() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getFeedItem().getFeedType().name(),
                        Collectors.summingDouble(FeedConsumption::getQuantity)
                ));

        List<FeedPurchase> purchases = feedPurchaseRepository.findAll().stream()
                .filter(p -> p.getPurchaseDate() != null && !p.getPurchaseDate().isBefore(start) && !p.getPurchaseDate().isAfter(end))
                .toList();

        Map<String, Double> purchasesBySupplier = purchases.stream()
                .filter(p -> p.getSupplier() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getSupplier().getSupplierName(),
                        Collectors.summingDouble(FeedPurchase::getTotalAmount)
                ));

        Map<String, Double> monthlyTrend = consumptionList.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getConsumptionDate().getYear() + "-" + String.format("%02d", c.getConsumptionDate().getMonthValue()),
                        Collectors.summingDouble(FeedConsumption::getQuantity)
                ));

        return FeedAnalyticsResponse.builder()
                .currentFeedStock(stock)
                .lowStockItemsCount(lowStockCount)
                .totalConsumedToday(dailyConsumed)
                .totalPurchaseCost(purchaseCost)
                .usageByType(usageByType)
                .purchasesBySupplier(purchasesBySupplier)
                .monthlyConsumptionTrend(monthlyTrend)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'sales_' + #dateFilter")
    public SalesAnalyticsResponse getSalesReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        log.info("AUDIT: Sales Report Viewed");
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        List<SalesOrder> ordersList = salesOrderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null && !o.getOrderDate().isBefore(start) && !o.getOrderDate().isAfter(end))
                .toList();

        double totalRevenue = ordersList.stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();

        double todaySales = salesService.getDailySales(LocalDate.now());
        double pendingPayments = salesOrderRepository.sumPendingPayments();

        double chickenSales = ordersList.stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getItemType() == ItemType.CHICKEN)
                .mapToDouble(SalesOrderItem::getTotalPrice)
                .sum();

        double eggSales = ordersList.stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getItemType() == ItemType.EGG_BATCH)
                .mapToDouble(SalesOrderItem::getTotalPrice)
                .sum();

        Map<String, Double> salesByCustomer = ordersList.stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED && o.getCustomer() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getCustomer().getCustomerName(),
                        Collectors.summingDouble(SalesOrder::getTotalAmount)
                ));

        Map<String, Double> monthlyTrend = ordersList.stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().getYear() + "-" + String.format("%02d", o.getOrderDate().getMonthValue()),
                        Collectors.summingDouble(SalesOrder::getTotalAmount)
                ));

        return SalesAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .todaySales(todaySales)
                .pendingPayments(pendingPayments)
                .salesByChicken(chickenSales)
                .salesByEggBatch(eggSales)
                .salesByCustomer(salesByCustomer)
                .monthlySalesTrend(monthlyTrend)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'finance_' + #dateFilter")
    public FinanceReportResponse getFinanceReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        log.info("AUDIT: Finance Report Viewed");
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        List<LedgerTransaction> txs = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate() != null && !t.getTransactionDate().isBefore(start) && !t.getTransactionDate().isAfter(end))
                .toList();

        double income = txs.stream().filter(t -> t.getTransactionType() == TransactionType.INCOME).mapToDouble(LedgerTransaction::getAmount).sum();
        double expense = txs.stream().filter(t -> t.getTransactionType() == TransactionType.EXPENSE).mapToDouble(LedgerTransaction::getAmount).sum();
        double netVal = income - expense;

        Map<String, Double> accountBalances = financeService.getAccountBalances();
        Map<String, Double> expenseByCategory = financeService.getExpenseByCategory();
        Map<String, Double> incomeByCategory = financeService.getIncomeByCategory();

        Map<String, Double> monthlyIncome = txs.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().getYear() + "-" + String.format("%02d", t.getTransactionDate().getMonthValue()),
                        Collectors.summingDouble(LedgerTransaction::getAmount)
                ));

        Map<String, Double> monthlyExpense = txs.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().getYear() + "-" + String.format("%02d", t.getTransactionDate().getMonthValue()),
                        Collectors.summingDouble(LedgerTransaction::getAmount)
                ));

        return FinanceReportResponse.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .netProfit(netVal)
                .accountBalances(accountBalances)
                .expenseByCategory(expenseByCategory)
                .incomeByCategory(incomeByCategory)
                .monthlyIncomeTrend(monthlyIncome)
                .monthlyExpenseTrend(monthlyExpense)
                .build();
    }

    @Override
    @Cacheable(value = "reports", key = "'profitability_' + #dateFilter")
    public ProfitabilityResponse getProfitabilityReport(String dateFilter, LocalDate customStart, LocalDate customEnd) {
        DateRange range = parseDateFilter(dateFilter, customStart, customEnd);
        LocalDate start = range.getStart();
        LocalDate end = range.getEnd();

        double revenue = salesOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED && !o.getOrderDate().isBefore(start) && !o.getOrderDate().isAfter(end))
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();

        double feedCost = feedPurchaseRepository.findAll().stream()
                .filter(p -> p.getPurchaseDate() != null && !p.getPurchaseDate().isBefore(start) && !p.getPurchaseDate().isAfter(end))
                .mapToDouble(FeedPurchase::getTotalAmount)
                .sum();

        double otherLedgerExpenses = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE && !t.getTransactionDate().isBefore(start) && !t.getTransactionDate().isAfter(end))
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();

        double grossProfit = revenue - feedCost;
        double netProfit = revenue - feedCost - otherLedgerExpenses;
        double margin = revenue > 0.0 ? (netProfit / revenue) * 100.0 : 0.0;

        return ProfitabilityResponse.builder()
                .totalRevenue(revenue)
                .feedPurchaseCost(feedCost)
                .otherExpenses(otherLedgerExpenses)
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .profitMarginPercentage(margin)
                .build();
    }
}
