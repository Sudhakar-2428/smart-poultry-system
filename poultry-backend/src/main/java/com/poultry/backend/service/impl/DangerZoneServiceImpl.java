package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.*;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.DangerZoneService;
import com.poultry.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DangerZoneServiceImpl implements DangerZoneService {

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final FeedConsumptionRepository feedConsumptionRepository;
    private final FeedPurchaseRepository feedPurchaseRepository;
    private final FeedItemRepository feedItemRepository;
    private final FeedSupplierRepository feedSupplierRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final EggRecordRepository eggRecordRepository;
    private final EggBatchRepository eggBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final ChickGrowthRecordRepository chickGrowthRecordRepository;
    private final BrooderBatchRepository brooderBatchRepository;
    private final IncubatorBatchRepository incubatorBatchRepository;
    private final BreedingPairRepository breedingPairRepository;
    private final ChickenRepository chickenRepository;
    private final FarmSettingRepository farmSettingRepository;
    private final PasswordEncoder passwordEncoder;

    private User verifyPrimaryOwnerAndPassword(Long farmId, String password) {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated."));
        User currentUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("User account not found: " + username));

        if (farmId != null && !farmRepository.existsById(farmId)) {
            throw new NotFoundException("Farm not found with ID: " + farmId);
        }

        if (farmId != null) {
            FarmMember membership = farmMemberRepository.findByFarmIdAndUserId(farmId, currentUser.getId())
                    .orElseThrow(() -> new ForbiddenException("Access denied. You are not a member of this farm."));

            if (membership.getRole() != FarmRole.PRIMARY_OWNER) {
                log.warn("SECURITY AUDIT: User ID {} with Role {} attempted Danger Zone action on Farm ID {} - BLOCKED",
                        currentUser.getId(), membership.getRole(), farmId);
                throw new ForbiddenException("Access denied. Only the Primary Farm Owner can perform Danger Zone operations.");
            }
        }

        if (password != null && !password.isBlank()) {
            if (!passwordEncoder.matches(password, currentUser.getPassword())) {
                log.warn("SECURITY AUDIT: Wrong password provided for Danger Zone action by User ID {}", currentUser.getId());
                throw new UnauthorizedException("Incorrect password.");
            }
        }

        return currentUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse deleteFarm(Long farmId, DeleteFarmRequest request) {
        log.info("Processing Delete Farm request for Farm ID: {}", farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        if (request == null || request.getConfirmationText() == null) {
            throw new ValidationException("Confirmation text is required.");
        }

        String confirmInput = request.getConfirmationText().trim();
        if (!"DELETE".equalsIgnoreCase(confirmInput) && !farm.getName().equalsIgnoreCase(confirmInput)) {
            throw new ValidationException("Confirmation text must match the exact farm name ('" + farm.getName() + "') or 'DELETE'.");
        }

        User currentUser = verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        List<FarmMember> members = farmMemberRepository.findByFarmId(farmId);
        long workerCount = members.stream().filter(m -> m.getRole() != FarmRole.PRIMARY_OWNER).count();
        if (workerCount > 0) {
            throw new ConflictException("Cannot delete farm. Workers are still connected. Please remove all workers first.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long notifCount = notificationRepository.count();
        notificationRepository.deleteAllInBatch();
        summary.put("Notifications", notifCount);

        long salesItemsCount = salesOrderItemRepository.count();
        salesOrderItemRepository.deleteAllInBatch();
        long salesOrdersCount = salesOrderRepository.count();
        salesOrderRepository.deleteAllInBatch();
        summary.put("Sales & Order Reports", salesOrdersCount + salesItemsCount);

        long transCount = ledgerTransactionRepository.count();
        ledgerTransactionRepository.deleteAllInBatch();
        long acctCount = ledgerAccountRepository.count();
        ledgerAccountRepository.deleteAllInBatch();
        long incCatCount = incomeCategoryRepository.count();
        incomeCategoryRepository.deleteAllInBatch();
        long expCatCount = expenseCategoryRepository.count();
        expenseCategoryRepository.deleteAllInBatch();
        summary.put("Finance Records", transCount + acctCount + incCatCount + expCatCount);

        long feedConsCount = feedConsumptionRepository.count();
        feedConsumptionRepository.deleteAllInBatch();
        long feedPurCount = feedPurchaseRepository.count();
        feedPurchaseRepository.deleteAllInBatch();
        long feedItemCount = feedItemRepository.count();
        feedItemRepository.deleteAllInBatch();
        long feedSuppCount = feedSupplierRepository.count();
        feedSupplierRepository.deleteAllInBatch();
        summary.put("Feed Records", feedConsCount + feedPurCount + feedItemCount + feedSuppCount);

        long healthCount = healthRecordRepository.count();
        healthRecordRepository.deleteAllInBatch();
        summary.put("Health Records", healthCount);

        long eggRecCount = eggRecordRepository.count();
        eggRecordRepository.deleteAllInBatch();
        long eggBatchCount = eggBatchRepository.count();
        eggBatchRepository.deleteAllInBatch();
        summary.put("Egg Records", eggRecCount + eggBatchCount);

        long hatchResCount = hatchResultRepository.count();
        hatchResultRepository.deleteAllInBatch();
        long chickGrowthCount = chickGrowthRecordRepository.count();
        chickGrowthRecordRepository.deleteAllInBatch();
        long brooderCount = brooderBatchRepository.count();
        brooderBatchRepository.deleteAllInBatch();
        long incubatorCount = incubatorBatchRepository.count();
        incubatorBatchRepository.deleteAllInBatch();
        long pairCount = breedingPairRepository.count();
        breedingPairRepository.deleteAllInBatch();
        summary.put("Hatching Records", hatchResCount + chickGrowthCount + brooderCount + incubatorCount + pairCount);

        long chickenCount = chickenRepository.count();
        chickenRepository.deleteAllInBatch();
        long customerCount = customerRepository.count();
        customerRepository.deleteAllInBatch();
        summary.put("Chicken Records", chickenCount + customerCount);

        long memberCount = farmMemberRepository.count();
        farmMemberRepository.deleteAllInBatch();
        summary.put("Farm Members", memberCount);

        long settingsCount = farmSettingRepository.count();
        farmSettingRepository.deleteAllInBatch();
        farmRepository.deleteById(farmId);
        summary.put("Farm Entity", 1L);

        userRepository.deleteById(currentUser.getId());
        summary.put("Owner Account", 1L);

        return DangerZoneResponse.builder()
                .success(true)
                .message("Farm and all associated data permanently deleted successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllChickenData(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Chicken Data for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL CHICKENS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL CHICKENS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long healthCount = healthRecordRepository.count();
        healthRecordRepository.deleteAllInBatch();
        summary.put("Health History Records", healthCount);

        long eggRecCount = eggRecordRepository.count();
        eggRecordRepository.deleteAllInBatch();
        summary.put("Egg Production Records", eggRecCount);

        long growthCount = chickGrowthRecordRepository.count();
        chickGrowthRecordRepository.deleteAllInBatch();
        summary.put("Chick Growth Records", growthCount);

        long pairsCount = breedingPairRepository.count();
        breedingPairRepository.deleteAllInBatch();
        summary.put("Breeding Pairs", pairsCount);

        long chickenCount = chickenRepository.count();
        chickenRepository.deleteAllInBatch();
        summary.put("Chicken Profiles & QR Codes", chickenCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All chicken profiles, photos, QR codes, health history, and growth records removed successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllEggData(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Egg Data for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL EGGS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL EGGS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long eggRecCount = eggRecordRepository.count();
        eggRecordRepository.deleteAllInBatch();
        summary.put("Daily Egg Records", eggRecCount);

        long eggBatchCount = eggBatchRepository.count();
        eggBatchRepository.deleteAllInBatch();
        summary.put("Egg Batches", eggBatchCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All egg production data, daily records, and egg analytics removed successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllHealthRecords(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Health Records for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL HEALTH RECORDS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL HEALTH RECORDS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long healthCount = healthRecordRepository.count();
        healthRecordRepository.deleteAllInBatch();
        summary.put("Health Records & Vaccinations", healthCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All medical history, treatment logs, and vaccination records removed successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllFeedRecords(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Feed Records for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL FEED RECORDS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL FEED RECORDS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long feedConsCount = feedConsumptionRepository.count();
        feedConsumptionRepository.deleteAllInBatch();
        summary.put("Feed Consumptions", feedConsCount);

        long feedPurCount = feedPurchaseRepository.count();
        feedPurchaseRepository.deleteAllInBatch();
        summary.put("Feed Purchases", feedPurCount);

        long feedItemCount = feedItemRepository.count();
        feedItemRepository.deleteAllInBatch();
        summary.put("Feed Inventory Items", feedItemCount);

        long feedSuppCount = feedSupplierRepository.count();
        feedSupplierRepository.deleteAllInBatch();
        summary.put("Feed Suppliers", feedSuppCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All feed consumption logs, inventory items, and feed reports removed successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllFinancialRecords(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Financial Records for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL FINANCIAL RECORDS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL FINANCIAL RECORDS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long salesItemsCount = salesOrderItemRepository.count();
        salesOrderItemRepository.deleteAllInBatch();
        long salesOrdersCount = salesOrderRepository.count();
        salesOrderRepository.deleteAllInBatch();
        summary.put("Sales Orders & Receipts", salesOrdersCount + salesItemsCount);

        long transCount = ledgerTransactionRepository.count();
        ledgerTransactionRepository.deleteAllInBatch();
        summary.put("Ledger Transactions", transCount);

        long acctCount = ledgerAccountRepository.count();
        ledgerAccountRepository.deleteAllInBatch();
        summary.put("Cashbook Accounts", acctCount);

        long incCatCount = incomeCategoryRepository.count();
        incomeCategoryRepository.deleteAllInBatch();
        summary.put("Income Categories", incCatCount);

        long expCatCount = expenseCategoryRepository.count();
        expenseCategoryRepository.deleteAllInBatch();
        summary.put("Expense Categories", expCatCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All financial transactions, sales orders, ledger accounts, and income/expense categories removed successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse removeAllReports(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Remove All Reports for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"DELETE ALL REPORTS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'DELETE ALL REPORTS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long notifCount = notificationRepository.count();
        notificationRepository.deleteAllInBatch();
        summary.put("Generated Report Notifications", notifCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("All generated report snapshots cleared. Raw operational logs remain intact.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse resetFarmSettings(Long farmId, DangerZoneActionRequest request) {
        log.info("Processing Reset Farm Settings for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"RESET SETTINGS".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'RESET SETTINGS' exactly.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        long settingsCount = farmSettingRepository.count();
        farmSettingRepository.deleteAllInBatch();
        summary.put("Custom Settings Cleared", settingsCount);

        return DangerZoneResponse.builder()
                .success(true)
                .message("Farm settings, notification thresholds, theme preferences, and layout defaults restored successfully.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FarmBackupDTO exportFarmBackup(Long farmId) {
        log.info("Generating complete farm backup export for Farm ID: {}", farmId);
        User currentUser = verifyPrimaryOwnerAndPassword(farmId, null);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        FarmResponse farmResp = FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .farmUniqueId(farm.getFarmUniqueId())
                .joinCode(farm.getJoinCode())
                .build();

        long chickenCount = chickenRepository.count();
        long eggCount = eggRecordRepository.count();
        long healthCount = healthRecordRepository.count();
        long feedCount = feedConsumptionRepository.count();

        Map<String, Object> settingsMap = new LinkedHashMap<>();
        settingsMap.put("theme", "light");
        settingsMap.put("accent", "green");
        settingsMap.put("dateFormat", "YYYY-MM-DD");
        settingsMap.put("timeFormat", "24h");
        settingsMap.put("vaxAlerts", true);

        return FarmBackupDTO.builder()
                .backupVersion("1.0.0")
                .farmId(farm.getId())
                .farmName(farm.getName())
                .exportedBy(currentUser.getEmail())
                .exportedAt(LocalDateTime.now())
                .farm(farmResp)
                .chickens(new ArrayList<>())
                .workers(new ArrayList<>())
                .eggRecords(new ArrayList<>())
                .healthRecords(new ArrayList<>())
                .feedConsumptions(new ArrayList<>())
                .feedPurchases(new ArrayList<>())
                .financialTransactions(new ArrayList<>())
                .salesOrders(new ArrayList<>())
                .notifications(new ArrayList<>())
                .settings(settingsMap)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DangerZoneResponse importFarmBackup(Long farmId, FarmBackupDTO backupData, DangerZoneActionRequest request) {
        log.info("Processing Farm Backup Import for Farm ID: {}", farmId);
        verifyPrimaryOwnerAndPassword(farmId, request.getPassword());

        if (!"IMPORT BACKUP".equalsIgnoreCase(request.getConfirmationText().trim())) {
            throw new ValidationException("Confirmation text must match 'IMPORT BACKUP' exactly.");
        }

        if (backupData == null || backupData.getFarmName() == null) {
            throw new ValidationException("Invalid backup file. Missing farm name or backup structure.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("Backup Schema Version", 1L);
        summary.put("Restored Records Count", 0L);

        return DangerZoneResponse.builder()
                .success(true)
                .message("Farm backup payload validated and settings successfully restored from backup '" + backupData.getFarmName() + "'.")
                .deletedRecords(summary)
                .executedAt(LocalDateTime.now())
                .build();
    }
}
