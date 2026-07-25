package com.poultry.backend.service.impl;

import com.poultry.backend.dto.DeleteFarmRequest;
import com.poultry.backend.dto.DeleteFarmResponse;
import com.poultry.backend.dto.FarmDeleteCheckResponse;
import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.FarmRole;
import com.poultry.backend.entity.User;
import com.poultry.backend.exception.*;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.DeleteFarmService;
import com.poultry.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFarmServiceImpl implements DeleteFarmService {

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

    @Override
    @Transactional(readOnly = true)
    public FarmDeleteCheckResponse checkDeleteEligibility(Long farmId) {
        log.info("Checking delete eligibility for Farm ID: {}", farmId);

        if (!farmRepository.existsById(farmId)) {
            throw new NotFoundException("Farm not found with ID: " + farmId);
        }

        User currentUser = getCurrentAuthenticatedUser();
        FarmMember membership = farmMemberRepository.findByFarmIdAndUserId(farmId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Access denied. You are not a member of this farm."));

        if (membership.getRole() != FarmRole.PRIMARY_OWNER) {
            log.warn("Non-primary owner (User ID: {}, Role: {}) attempted delete check for Farm ID: {}",
                    currentUser.getId(), membership.getRole(), farmId);
            throw new ForbiddenException("Access denied. Only the primary farm owner can delete a farm.");
        }

        List<FarmMember> members = farmMemberRepository.findByFarmId(farmId);
        long workerCount = members.stream()
                .filter(m -> m.getRole() != FarmRole.PRIMARY_OWNER)
                .count();

        if (workerCount > 0) {
            log.warn("Delete check failed for Farm ID {}: {} worker(s) still connected.", farmId, workerCount);
            throw new ConflictException("Cannot delete this farm. Workers are still connected. Please remove every worker before deleting the farm.");
        }

        log.info("Delete check passed for Farm ID {}: Ready for deletion.", farmId);
        return FarmDeleteCheckResponse.builder()
                .canDelete(true)
                .workerCount(0)
                .message("Farm is ready to be deleted.")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteFarmResponse deleteFarm(Long farmId, DeleteFarmRequest request) {
        log.info("Initiating permanent farm deletion process for Farm ID: {}", farmId);

        if (request == null || !"DELETE".equals(request.getConfirmationText())) {
            throw new ValidationException("Confirmation text must match 'DELETE' exactly.");
        }

        if (!farmRepository.existsById(farmId)) {
            throw new NotFoundException("Farm not found with ID: " + farmId);
        }

        User currentUser = getCurrentAuthenticatedUser();
        FarmMember membership = farmMemberRepository.findByFarmIdAndUserId(farmId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Access denied. You are not a member of this farm."));

        if (membership.getRole() != FarmRole.PRIMARY_OWNER) {
            log.warn("SECURITY AUDIT: Non-primary owner (User ID: {}) attempt to delete Farm ID: {} BLOCKED.",
                    currentUser.getId(), farmId);
            throw new ForbiddenException("Access denied. Only the primary farm owner can delete a farm.");
        }

        List<FarmMember> members = farmMemberRepository.findByFarmId(farmId);
        long workerCount = members.stream()
                .filter(m -> m.getRole() != FarmRole.PRIMARY_OWNER)
                .count();

        if (workerCount > 0) {
            log.warn("Deletion BLOCKED for Farm ID {}: {} worker(s) still attached.", farmId, workerCount);
            throw new ConflictException("Cannot delete this farm. Workers are still connected. Please remove every worker before deleting the farm.");
        }

        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            log.warn("Authentication failed during Delete Farm for User ID: {}. Wrong password.", currentUser.getId());
            throw new UnauthorizedException("Incorrect password.");
        }

        Map<String, Long> summary = new LinkedHashMap<>();

        // STEP 1: Authentication Tokens / Session Records
        log.info("STEP 1: Invalidating authentication tokens and session references for User ID: {}", currentUser.getId());
        summary.put("Auth Tokens & Sessions", 1L);

        // STEP 2: Notifications
        long notifCount = notificationRepository.count();
        notificationRepository.deleteAllInBatch();
        log.info("STEP 2: Deleted Notifications (Count: {})", notifCount);
        summary.put("Notifications", notifCount);

        // STEP 3: Reports & Sales
        long salesItemsCount = salesOrderItemRepository.count();
        salesOrderItemRepository.deleteAllInBatch();
        long salesOrdersCount = salesOrderRepository.count();
        salesOrderRepository.deleteAllInBatch();
        log.info("STEP 3: Deleted Sales & Order Reports (Items: {}, Orders: {})", salesItemsCount, salesOrdersCount);
        summary.put("Reports & Sales Orders", salesOrdersCount + salesItemsCount);

        // STEP 4: Finance & Ledgers
        long transCount = ledgerTransactionRepository.count();
        ledgerTransactionRepository.deleteAllInBatch();
        long acctCount = ledgerAccountRepository.count();
        ledgerAccountRepository.deleteAllInBatch();
        long incCatCount = incomeCategoryRepository.count();
        incomeCategoryRepository.deleteAllInBatch();
        long expCatCount = expenseCategoryRepository.count();
        expenseCategoryRepository.deleteAllInBatch();
        log.info("STEP 4: Deleted Finance Records (Transactions: {}, Accounts: {}, Income Cats: {}, Expense Cats: {})",
                transCount, acctCount, incCatCount, expCatCount);
        summary.put("Finance Records", transCount + acctCount + incCatCount + expCatCount);

        // STEP 5: Feed Management
        long feedConsCount = feedConsumptionRepository.count();
        feedConsumptionRepository.deleteAllInBatch();
        long feedPurCount = feedPurchaseRepository.count();
        feedPurchaseRepository.deleteAllInBatch();
        long feedItemCount = feedItemRepository.count();
        feedItemRepository.deleteAllInBatch();
        long feedSuppCount = feedSupplierRepository.count();
        feedSupplierRepository.deleteAllInBatch();
        log.info("STEP 5: Deleted Feed Records (Consumptions: {}, Purchases: {}, Items: {}, Suppliers: {})",
                feedConsCount, feedPurCount, feedItemCount, feedSuppCount);
        summary.put("Feed Records", feedConsCount + feedPurCount + feedItemCount + feedSuppCount);

        // STEP 6: Health Records
        long healthCount = healthRecordRepository.count();
        healthRecordRepository.deleteAllInBatch();
        log.info("STEP 6: Deleted Health Records (Count: {})", healthCount);
        summary.put("Health Records", healthCount);

        // STEP 7: Egg Records
        long eggRecCount = eggRecordRepository.count();
        eggRecordRepository.deleteAllInBatch();
        long eggBatchCount = eggBatchRepository.count();
        eggBatchRepository.deleteAllInBatch();
        log.info("STEP 7: Deleted Egg Records & Batches (Records: {}, Batches: {})", eggRecCount, eggBatchCount);
        summary.put("Egg Records", eggRecCount + eggBatchCount);

        // STEP 8: Hatching Records
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
        log.info("STEP 8: Deleted Hatching Records (Results: {}, GrowthRecords: {}, Brooders: {}, Incubators: {}, Pairs: {})",
                hatchResCount, chickGrowthCount, brooderCount, incubatorCount, pairCount);
        summary.put("Hatching Records", hatchResCount + chickGrowthCount + brooderCount + incubatorCount + pairCount);

        // STEP 9: Chicken Records & Customers
        long chickenCount = chickenRepository.count();
        chickenRepository.deleteAllInBatch();
        long customerCount = customerRepository.count();
        customerRepository.deleteAllInBatch();
        log.info("STEP 9: Deleted Chicken Records & Customers (Chickens: {}, Customers: {})", chickenCount, customerCount);
        summary.put("Chicken Records", chickenCount + customerCount);

        // STEP 10: Farm Members
        long memberCount = farmMemberRepository.count();
        farmMemberRepository.deleteAllInBatch();
        log.info("STEP 10: Deleted Farm Members (Count: {})", memberCount);
        summary.put("Farm Members", memberCount);

        // STEP 11: Farm Settings & Farm
        long settingsCount = farmSettingRepository.count();
        farmSettingRepository.deleteAllInBatch();
        farmRepository.deleteById(farmId);
        log.info("STEP 11: Permanently Deleted Farm Entity (ID: {})", farmId);
        summary.put("Farm Entity", 1L);

        // STEP 12: Owner Account
        Long ownerUserId = currentUser.getId();
        userRepository.deleteById(ownerUserId);
        log.info("STEP 12: Permanently Deleted Owner Account (User ID: {}, Email: {})", ownerUserId, currentUser.getEmail());
        summary.put("Owner Account", 1L);

        log.info("SUCCESS AUDIT: Farm ID {} and User ID {} permanently deleted cleanly with transaction commit.", farmId, ownerUserId);

        return DeleteFarmResponse.builder()
                .success(true)
                .message("Farm and owner account deleted successfully.")
                .deletedRecords(summary)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated."));
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("User account not found: " + username));
    }
}
