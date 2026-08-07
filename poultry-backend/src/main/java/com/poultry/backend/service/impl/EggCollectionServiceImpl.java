package com.poultry.backend.service.impl;

import com.poultry.backend.dto.EggCollectionDTOs.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.EggCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EggCollectionServiceImpl implements EggCollectionService {

    private final EggCollectionRepository eggCollectionRepository;
    private final EggItemRepository eggItemRepository;
    private final EggRecordRepository eggRecordRepository;
    private final EggBatchRepository eggBatchRepository;
    private final IncubatorBatchRepository incubatorBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final ChickenRepository chickenRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public EggCollectionResponse createEggCollectionFromPairing(BreedingPair pair) {
        log.info("Creating Egg Collection record for pairing ID: {}, pair code: {}", pair.getId(), pair.getPairCode());
        Chicken female = pair.getFemaleChicken();
        if (female == null) {
            throw new ValidationException("Female chicken (Hen) is required for Egg Collection.");
        }

        // Check existing collections for this hen to calculate batch number
        List<EggCollection> existing = eggCollectionRepository.findAll((root, query, cb) ->
                cb.equal(root.get("femaleChicken").get("id"), female.getId())
        );

        int batchNumber = existing.size() + 1;

        // Deactivate previous active collections for this hen if any
        existing.stream()
                .filter(ec -> ec.getStatus() == EggCollectionStatus.ACTIVE)
                .forEach(ec -> {
                    ec.setStatus(EggCollectionStatus.COMPLETED);
                    eggCollectionRepository.save(ec);
                });

        EggCollection collection = EggCollection.builder()
                .femaleChicken(female)
                .maleChicken(pair.getMaleChicken())
                .breedingPair(pair)
                .pairingDate(pair.getStartDate())
                .eggLayingStartedDate(LocalDate.now())
                .currentBatchNumber(batchNumber)
                .todayEggCount(0)
                .weeklyEggCount(0)
                .monthlyEggCount(0)
                .totalEggCount(0)
                .status(EggCollectionStatus.ACTIVE)
                .remarks("Egg laying initialized from pairing " + pair.getPairCode())
                .build();

        EggCollection savedCollection = eggCollectionRepository.save(collection);

        // Generate Automatic Egg Batch (EB-HenCode-BatchNum e.g. EB-101-03)
        String henCodeStr = female.getChickenCode() != null ? female.getChickenCode() : String.valueOf(female.getId());
        String batchCode = String.format("EB-%s-%02d", henCodeStr, batchNumber);

        Optional<EggBatch> existingBatch = eggBatchRepository.findByBatchCode(batchCode);
        if (existingBatch.isEmpty()) {
            EggBatch eggBatch = EggBatch.builder()
                    .batchCode(batchCode)
                    .batchDate(LocalDate.now())
                    .sourceHen(female)
                    .maleChicken(pair.getMaleChicken())
                    .breedingPair(pair)
                    .batchNumber(batchNumber)
                    .totalEggs(0)
                    .goodEggs(0)
                    .damagedEggs(0)
                    .brokenEggs(0)
                    .crackedEggs(0)
                    .doubleYolkEggs(0)
                    .selectedForHatching(0)
                    .selectedForSale(0)
                    .selectedForHomeUse(0)
                    .status(EggBatchStatus.ACTIVE)
                    .purpose(EggPurpose.HATCHING)
                    .remarks("Automatic egg batch for cycle " + batchNumber)
                    .build();
            eggBatchRepository.save(eggBatch);
        }

        // Record timeline event
        recordTimelineEvent(female, "EGG_COLLECTION_STARTED", "Egg Collection Started",
                "Hen transferred to Egg Collection Module for Batch " + batchNumber + " (Pairing " + pair.getPairCode() + ")");

        if (pair.getMaleChicken() != null) {
            recordTimelineEvent(pair.getMaleChicken(), "EGG_COLLECTION_STARTED", "Egg Laying Started with Partner",
                    "Partner Hen " + female.getChickenCode() + " initiated Egg Collection Batch " + batchNumber);
        }

        createNotification("Egg Collection Initiated",
                "Egg Collection record created for Hen " + female.getChickenCode() + " (Batch " + batchNumber + ").",
                NotificationType.BREEDING, Severity.INFO, savedCollection.getId());

        return toCollectionResponse(savedCollection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggCollectionResponse> getActiveLayingHens(Pageable pageable) {
        Page<EggCollection> page = eggCollectionRepository.findByStatus(EggCollectionStatus.ACTIVE, pageable);
        List<EggCollectionResponse> responses = page.getContent().stream().map(this::toCollectionResponse).toList();
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EggCollectionResponse getCollectionById(Long id) {
        EggCollection collection = eggCollectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg collection record not found with ID: " + id));
        return toCollectionResponse(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public HenLayingProfileResponse getHenLayingProfile(Long henId) {
        Chicken hen = chickenRepository.findById(henId)
                .orElseThrow(() -> new NotFoundException("Hen not found with ID: " + henId));

        EggCollection activeCol = eggCollectionRepository.findByFemaleChickenIdAndStatus(henId, EggCollectionStatus.ACTIVE)
                .orElse(null);

        List<BatchSummaryResponse> batchHistory = getHenBatchHistory(henId);
        int totalEggs = batchHistory.stream().mapToInt(b -> b.getTotalEggs() != null ? b.getTotalEggs() : 0).sum();

        Chicken male = activeCol != null ? activeCol.getMaleChicken() : null;

        return HenLayingProfileResponse.builder()
                .henId(hen.getId())
                .henCode(hen.getChickenCode())
                .henName(hen.getName())
                .henBreed(hen.getBreed() != null ? hen.getBreed().name() : "COUNTRY_CHICKEN")
                .age(hen.getDateOfBirth() != null ? ChronoUnit.MONTHS.between(hen.getDateOfBirth(), LocalDate.now()) + " mos" : "N/A")
                .weight(hen.getWeight())
                .healthStatus(hen.getHealthStatus() != null ? hen.getHealthStatus().name() : "HEALTHY")
                .roosterId(male != null ? male.getId() : null)
                .roosterCode(male != null ? male.getChickenCode() : null)
                .roosterName(male != null ? male.getName() : null)
                .pairingDate(activeCol != null ? activeCol.getPairingDate() : null)
                .eggLayingStartedDate(activeCol != null ? activeCol.getEggLayingStartedDate() : null)
                .currentBatchNumber(activeCol != null ? activeCol.getCurrentBatchNumber() : (batchHistory.size()))
                .totalEggs(totalEggs)
                .batchHistory(batchHistory)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoosterLayingProfileResponse getRoosterLayingProfile(Long roosterId) {
        Chicken rooster = chickenRepository.findById(roosterId)
                .orElseThrow(() -> new NotFoundException("Rooster not found with ID: " + roosterId));

        List<EggCollection> collections = eggCollectionRepository.findAll((root, query, cb) ->
                cb.equal(root.get("maleChicken").get("id"), roosterId)
        );

        Map<Long, Chicken> partnerHens = new HashMap<>();
        for (EggCollection col : collections) {
            if (col.getFemaleChicken() != null) {
                partnerHens.put(col.getFemaleChicken().getId(), col.getFemaleChicken());
            }
        }

        List<RoosterHenSummary> summaries = new ArrayList<>();
        int grandTotalEggs = 0;
        int grandTotalHatching = 0;
        int grandTotalHatchedChicks = 0;

        for (Chicken hen : partnerHens.values()) {
            List<EggItem> henEggs = eggItemRepository.findByFemaleChickenId(hen.getId()).stream()
                    .filter(i -> i.getMaleChicken() != null && i.getMaleChicken().getId().equals(roosterId))
                    .toList();

            int eggCount = henEggs.size();
            int hatchingCount = (int) henEggs.stream().filter(i -> i.getPurpose() == EggPurpose.HATCHING).count();

            // Count hatched chicks if incubator batches exist
            long hatchedCount = incubatorBatchRepository.findAll().stream()
                    .filter(ib -> ib.getEggBatch() != null && ib.getEggBatch().getSourceHen() != null && ib.getEggBatch().getSourceHen().getId().equals(hen.getId()))
                    .mapToLong(ib -> hatchResultRepository.findByIncubatorBatchId(ib.getId())
                            .map(hr -> hr.getHatchedChicks() != null ? (long) hr.getHatchedChicks() : 0L)
                            .orElse(0L))
                    .sum();

            grandTotalEggs += eggCount;
            grandTotalHatching += hatchingCount;
            grandTotalHatchedChicks += (int) hatchedCount;

            EggCollection activeCol = eggCollectionRepository.findByFemaleChickenIdAndStatus(hen.getId(), EggCollectionStatus.ACTIVE).orElse(null);

            summaries.add(RoosterHenSummary.builder()
                    .henId(hen.getId())
                    .henCode(hen.getChickenCode())
                    .henName(hen.getName())
                    .henBreed(hen.getBreed() != null ? hen.getBreed().name() : "COUNTRY_CHICKEN")
                    .currentBatch(activeCol != null ? activeCol.getCurrentBatchNumber() : 1)
                    .totalEggsCollected(eggCount)
                    .eggsSelectedForHatching(hatchingCount)
                    .hatchedChicks((int) hatchedCount)
                    .build());
        }

        return RoosterLayingProfileResponse.builder()
                .roosterId(rooster.getId())
                .roosterCode(rooster.getChickenCode())
                .roosterName(rooster.getName())
                .roosterBreed(rooster.getBreed() != null ? rooster.getBreed().name() : "COUNTRY_CHICKEN")
                .totalPartnerHens(partnerHens.size())
                .totalEggsCollected(grandTotalEggs)
                .totalEggsSelectedForHatching(grandTotalHatching)
                .totalHatchedChicks(grandTotalHatchedChicks)
                .henSummaries(summaries)
                .build();
    }

    @Override
    @Transactional
    public EggCollectionResponse recordDailyEggs(DailyEggRecordRequest request) {
        EggCollection collection = eggCollectionRepository.findById(request.getEggCollectionId())
                .orElseThrow(() -> new NotFoundException("Egg collection record not found with ID: " + request.getEggCollectionId()));

        int totalCollected = request.getNumberOfEggs();
        int broken = request.getBrokenEggs() != null ? request.getBrokenEggs() : 0;
        int damaged = request.getDamagedEggs() != null ? request.getDamagedEggs() : 0;
        int cracked = request.getCrackedEggs() != null ? request.getCrackedEggs() : 0;
        int doubleYolk = request.getDoubleYolkEggs() != null ? request.getDoubleYolkEggs() : 0;

        int defectiveCount = broken + damaged + cracked;
        int healthy = totalCollected - defectiveCount;

        if (healthy < 0) {
            throw new ValidationException("Defective eggs (broken + damaged + cracked) cannot exceed total eggs collected.");
        }

        LocalDate recordDate = request.getCollectionDate() != null ? request.getCollectionDate() : LocalDate.now();
        EggPurpose mainPurpose = request.getPurpose() != null ? request.getPurpose() : EggPurpose.HATCHING;

        // 1. Update collection aggregations
        collection.setTodayEggCount(totalCollected);
        collection.setWeeklyEggCount(collection.getWeeklyEggCount() + totalCollected);
        collection.setMonthlyEggCount(collection.getMonthlyEggCount() + totalCollected);
        collection.setTotalEggCount(collection.getTotalEggCount() + totalCollected);
        EggCollection updatedCollection = eggCollectionRepository.save(collection);

        // 2. Fetch or create EggBatch for this cycle
        Chicken female = collection.getFemaleChicken();
        int batchNum = collection.getCurrentBatchNumber();
        String henCodeStr = female != null ? female.getChickenCode() : String.valueOf(female.getId());
        String batchCode = String.format("EB-%s-%02d", henCodeStr, batchNum);

        EggBatch eggBatch = eggBatchRepository.findByBatchCode(batchCode)
                .orElseGet(() -> eggBatchRepository.save(EggBatch.builder()
                        .batchCode(batchCode)
                        .batchDate(recordDate)
                        .sourceHen(female)
                        .maleChicken(collection.getMaleChicken())
                        .breedingPair(collection.getBreedingPair())
                        .batchNumber(batchNum)
                        .totalEggs(0)
                        .goodEggs(0)
                        .damagedEggs(0)
                        .brokenEggs(0)
                        .crackedEggs(0)
                        .doubleYolkEggs(0)
                        .selectedForHatching(0)
                        .selectedForSale(0)
                        .selectedForHomeUse(0)
                        .status(EggBatchStatus.ACTIVE)
                        .purpose(mainPurpose)
                        .build()));

        // Update EggBatch metrics
        eggBatch.setTotalEggs(eggBatch.getTotalEggs() + totalCollected);
        eggBatch.setGoodEggs(eggBatch.getGoodEggs() + Math.max(0, healthy));
        eggBatch.setBrokenEggs(eggBatch.getBrokenEggs() + broken);
        eggBatch.setDamagedEggs(eggBatch.getDamagedEggs() + damaged);
        eggBatch.setCrackedEggs(eggBatch.getCrackedEggs() + cracked);
        eggBatch.setDoubleYolkEggs(eggBatch.getDoubleYolkEggs() + doubleYolk);

        if (mainPurpose == EggPurpose.HATCHING) {
            eggBatch.setSelectedForHatching(eggBatch.getSelectedForHatching() + Math.max(0, healthy));
        } else if (mainPurpose == EggPurpose.MARKET || mainPurpose == EggPurpose.SALE) {
            eggBatch.setSelectedForSale(eggBatch.getSelectedForSale() + Math.max(0, healthy));
        } else if (mainPurpose == EggPurpose.HOME_CONSUMPTION || mainPurpose == EggPurpose.CONSUMPTION) {
            eggBatch.setSelectedForHomeUse(eggBatch.getSelectedForHomeUse() + Math.max(0, healthy));
        }
        eggBatchRepository.save(eggBatch);

        // 3. Save EggRecord for historical reporting
        EggRecord eggRecord = EggRecord.builder()
                .hen(female)
                .recordDate(recordDate)
                .numberOfEggs(totalCollected)
                .damagedEggs(defectiveCount)
                .remarks(request.getRemarks())
                .build();
        eggRecordRepository.save(eggRecord);

        // 4. Generate individual unique EggItem records (EB-101-03-001, EB-101-03-002...)
        List<EggItem> existingBatchEggs = eggItemRepository.findByFemaleChickenIdAndBatchNumber(female.getId(), batchNum);
        int currentBatchEggCount = existingBatchEggs.size();

        List<EggItem> itemsToSave = new ArrayList<>();

        for (int i = 1; i <= totalCollected; i++) {
            int seq = currentBatchEggCount + i;
            String eggCode = String.format("EB-%s-%02d-%03d", henCodeStr, batchNum, seq);

            EggQuality quality;
            EggPurpose purpose;
            EggItemStatus itemStatus;

            if (i <= broken) {
                quality = EggQuality.BROKEN;
                purpose = EggPurpose.BROKEN;
                itemStatus = EggItemStatus.DISCARDED;
            } else if (i <= broken + damaged) {
                quality = EggQuality.DAMAGED;
                purpose = EggPurpose.REJECTED;
                itemStatus = EggItemStatus.DISCARDED;
            } else if (i <= broken + damaged + cracked) {
                quality = EggQuality.CRACKED;
                purpose = EggPurpose.REJECTED;
                itemStatus = EggItemStatus.DISCARDED;
            } else if (i <= broken + damaged + cracked + doubleYolk) {
                quality = EggQuality.DOUBLE_YOLK;
                purpose = mainPurpose;
                itemStatus = EggItemStatus.COLLECTED;
            } else {
                quality = EggQuality.HEALTHY;
                purpose = mainPurpose;
                itemStatus = EggItemStatus.COLLECTED;
            }

            EggItem item = EggItem.builder()
                    .eggCode(eggCode)
                    .femaleChicken(female)
                    .maleChicken(collection.getMaleChicken())
                    .breedingPair(collection.getBreedingPair())
                    .eggCollection(collection)
                    .eggBatch(eggBatch)
                    .batchNumber(batchNum)
                    .collectionDate(recordDate)
                    .status(itemStatus)
                    .purpose(purpose)
                    .eggQuality(quality)
                    .isMovedToHatching(false)
                    .remarks(request.getRemarks())
                    .build();
            itemsToSave.add(item);
        }
        eggItemRepository.saveAll(itemsToSave);

        // 5. Timeline & Notifications
        recordTimelineEvent(female, "EGGS_COLLECTED", "Eggs Collected",
                "Recorded " + totalCollected + " eggs (Healthy: " + healthy + ", Broken: " + broken + ") for Batch " + batchCode);

        if (collection.getMaleChicken() != null) {
            recordTimelineEvent(collection.getMaleChicken(), "EGGS_COLLECTED", "Eggs Collected from Partner Hen",
                    "Recorded " + totalCollected + " eggs from Partner Hen " + female.getChickenCode() + " for Batch " + batchCode);
        }

        createNotification("Eggs Collected Successfully",
                "Recorded " + totalCollected + " eggs for Hen " + female.getChickenCode() + " (" + batchCode + ").",
                NotificationType.BREEDING, Severity.INFO, collection.getId());

        return toCollectionResponse(updatedCollection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggItemResponse> getEggItems(String category, String breed, String purpose, Pageable pageable) {
        Specification<EggItem> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (purpose != null && !purpose.isBlank() && !"ALL".equalsIgnoreCase(purpose)) {
                predicates.add(cb.equal(root.get("purpose"), EggPurpose.valueOf(purpose.toUpperCase())));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<EggItem> page = eggItemRepository.findAll(spec, pageable);
        List<EggItemResponse> list = page.getContent().stream().map(this::toEggItemResponse).toList();
        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public EggItemResponse updateEggPurpose(UpdateEggPurposeRequest request) {
        EggItem item = eggItemRepository.findById(request.getEggId())
                .orElseThrow(() -> new NotFoundException("Egg item not found with ID: " + request.getEggId()));

        EggPurpose oldPurpose = item.getPurpose();
        item.setPurpose(request.getPurpose());

        if (request.getPurpose() == EggPurpose.BROKEN || request.getPurpose() == EggPurpose.REJECTED) {
            item.setStatus(EggItemStatus.DISCARDED);
        } else if (request.getPurpose() == EggPurpose.MARKET || request.getPurpose() == EggPurpose.SALE) {
            item.setStatus(EggItemStatus.COLLECTED);
        } else if (request.getPurpose() == EggPurpose.HOME_CONSUMPTION || request.getPurpose() == EggPurpose.CONSUMPTION) {
            item.setStatus(EggItemStatus.CONSUMED);
        }

        EggItem saved = eggItemRepository.save(item);

        if (saved.getFemaleChicken() != null) {
            recordTimelineEvent(saved.getFemaleChicken(), "EGG_PURPOSE_ASSIGNED", "Egg Purpose Assigned",
                    "Egg " + saved.getEggCode() + " purpose updated from " + oldPurpose + " to " + saved.getPurpose());
        }

        return toEggItemResponse(saved);
    }

    @Override
    @Transactional
    public int sendEggsToHatching(SendToHatchingRequest request) {
        if (request.getEggIds() == null || request.getEggIds().isEmpty()) {
            throw new ValidationException("Please select at least one egg to send to hatching.");
        }

        List<EggItem> eggs = eggItemRepository.findByIdIn(request.getEggIds());
        if (eggs.isEmpty()) {
            throw new NotFoundException("No valid eggs found for the provided IDs.");
        }

        String batchCode = request.getTargetBatchCode() != null && !request.getTargetBatchCode().isBlank()
                ? request.getTargetBatchCode()
                : "INC-" + LocalDate.now().getYear() + "-" + (incubatorBatchRepository.count() + 1);

        Chicken primaryHen = eggs.get(0).getFemaleChicken();

        EggBatch eggBatch = EggBatch.builder()
                .batchCode("EGGB-" + batchCode)
                .batchDate(LocalDate.now())
                .sourceHen(primaryHen)
                .maleChicken(eggs.get(0).getMaleChicken())
                .breedingPair(eggs.get(0).getBreedingPair())
                .totalEggs(eggs.size())
                .goodEggs(eggs.size())
                .damagedEggs(0)
                .status(EggBatchStatus.INCUBATING)
                .purpose(EggPurpose.HATCHING)
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .remarks("Batch created from Egg Collection handoff")
                .build();
        EggBatch savedEggBatch = eggBatchRepository.save(eggBatch);

        IncubatorBatch batch = IncubatorBatch.builder()
                .batchCode(batchCode)
                .eggBatch(savedEggBatch)
                .startDate(LocalDate.now())
                .expectedHatchDate(LocalDate.now().plusDays(21))
                .status(IncubatorStatus.ACTIVE)
                .temperature(37.5)
                .humidity(55.0)
                .notes(request.getRemarks() != null ? request.getRemarks() : "Transferred from Egg Collection Module")
                .build();

        incubatorBatchRepository.save(batch);

        for (EggItem egg : eggs) {
            egg.setPurpose(EggPurpose.HATCHING);
            egg.setStatus(EggItemStatus.SENT_TO_HATCHING);
            egg.setIsMovedToHatching(true);
            eggItemRepository.save(egg);
        }

        if (primaryHen != null) {
            recordTimelineEvent(primaryHen, "MOVED_TO_HATCHING", "Moved To Hatching",
                    "Transferred " + eggs.size() + " eggs to Incubator Batch " + batchCode);
        }

        createNotification("Eggs Transferred to Hatching",
                "Successfully transferred " + eggs.size() + " eggs to Incubator Batch " + batchCode + ".",
                NotificationType.BREEDING, Severity.INFO, batch.getId());

        return eggs.size();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long activeHens = eggCollectionRepository.count((root, query, cb) -> cb.equal(root.get("status"), EggCollectionStatus.ACTIVE));
        LocalDate today = LocalDate.now();

        long todayEggs = eggItemRepository.countByCollectionDate(today);
        long weeklyEggs = eggItemRepository.countByCollectionDateBetween(today.minusDays(7), today);
        long monthlyEggs = eggItemRepository.countByCollectionDateBetween(today.minusDays(30), today);

        long hatchingEggs = eggItemRepository.countByPurpose(EggPurpose.HATCHING);
        long saleEggs = eggItemRepository.countByPurposeIn(List.of(EggPurpose.MARKET, EggPurpose.SALE));
        long homeEggs = eggItemRepository.countByPurposeIn(List.of(EggPurpose.HOME_CONSUMPTION, EggPurpose.CONSUMPTION));
        long brokenEggs = eggItemRepository.countByPurposeIn(List.of(EggPurpose.BROKEN, EggPurpose.REJECTED));

        double avgEggs = activeHens > 0 ? (double) todayEggs / activeHens : 0.0;

        return DashboardStatsResponse.builder()
                .totalActiveLayingHens(activeHens)
                .todayEggs(todayEggs)
                .weeklyEggs(weeklyEggs)
                .monthlyEggs(monthlyEggs)
                .eggsForHatching(hatchingEggs)
                .eggsForSale(saleEggs)
                .eggsForHomeUse(homeEggs)
                .brokenEggs(brokenEggs)
                .averageEggsPerHen(Math.round(avgEggs * 100.0) / 100.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchSummaryResponse> getHenBatchHistory(Long henId) {
        List<EggCollection> collections = eggCollectionRepository.findAll((root, query, cb) ->
                cb.equal(root.get("femaleChicken").get("id"), henId)
        );

        List<BatchSummaryResponse> result = new ArrayList<>();
        for (EggCollection col : collections) {
            List<EggItem> items = eggItemRepository.findByFemaleChickenIdAndBatchNumber(henId, col.getCurrentBatchNumber());

            int total = items.size();
            int broken = (int) items.stream().filter(i -> i.getPurpose() == EggPurpose.BROKEN || i.getPurpose() == EggPurpose.REJECTED).count();
            int healthy = total - broken;
            int hatching = (int) items.stream().filter(i -> i.getPurpose() == EggPurpose.HATCHING).count();
            int sale = (int) items.stream().filter(i -> i.getPurpose() == EggPurpose.MARKET || i.getPurpose() == EggPurpose.SALE).count();
            int home = (int) items.stream().filter(i -> i.getPurpose() == EggPurpose.HOME_CONSUMPTION || i.getPurpose() == EggPurpose.CONSUMPTION).count();

            String henCodeStr = col.getFemaleChicken() != null ? col.getFemaleChicken().getChickenCode() : String.valueOf(henId);
            String batchCode = String.format("EB-%s-%02d", henCodeStr, col.getCurrentBatchNumber());

            result.add(BatchSummaryResponse.builder()
                    .batchCode(batchCode)
                    .batchNumber(col.getCurrentBatchNumber())
                    .startDate(col.getEggLayingStartedDate())
                    .endDate(col.getStatus() == EggCollectionStatus.COMPLETED ? col.getUpdatedAt().toLocalDate() : null)
                    .totalEggs(total)
                    .healthyEggs(healthy)
                    .brokenEggs(broken)
                    .damagedEggs(0)
                    .crackedEggs(0)
                    .doubleYolkEggs(0)
                    .selectedForHatching(hatching)
                    .selectedForSale(sale)
                    .selectedForHomeUse(home)
                    .batchStatus(col.getStatus() != null ? col.getStatus().name() : "ACTIVE")
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchSummaryResponse> getEggBatches(Pageable pageable) {
        Page<EggBatch> page = eggBatchRepository.findAll(pageable);
        List<BatchSummaryResponse> list = page.getContent().stream().map(b -> BatchSummaryResponse.builder()
                .batchCode(b.getBatchCode())
                .batchNumber(b.getBatchNumber() != null ? b.getBatchNumber() : 1)
                .startDate(b.getBatchDate())
                .endDate(b.getActualHatchDate())
                .totalEggs(b.getTotalEggs())
                .healthyEggs(b.getGoodEggs())
                .brokenEggs(b.getBrokenEggs())
                .damagedEggs(b.getDamagedEggs())
                .crackedEggs(b.getCrackedEggs())
                .doubleYolkEggs(b.getDoubleYolkEggs())
                .selectedForHatching(b.getSelectedForHatching())
                .selectedForSale(b.getSelectedForSale())
                .selectedForHomeUse(b.getSelectedForHomeUse())
                .batchStatus(b.getStatus() != null ? b.getStatus().name() : "ACTIVE")
                .build()
        ).toList();

        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    private EggCollectionResponse toCollectionResponse(EggCollection col) {
        Chicken female = col.getFemaleChicken();
        Chicken male = col.getMaleChicken();
        BreedingPair pair = col.getBreedingPair();

        long daysSince = col.getPairingDate() != null ? ChronoUnit.DAYS.between(col.getPairingDate(), LocalDate.now()) : 0;

        String henCodeStr = female != null ? female.getChickenCode() : "H";
        String batchCode = String.format("EB-%s-%02d", henCodeStr, col.getCurrentBatchNumber());

        return EggCollectionResponse.builder()
                .id(col.getId())
                .femaleChickenId(female != null ? female.getId() : null)
                .femaleChickenCode(female != null ? female.getChickenCode() : null)
                .femaleChickenName(female != null ? female.getName() : null)
                .femaleChickenBreed(female != null && female.getBreed() != null ? female.getBreed().name() : "COUNTRY_CHICKEN")
                .femaleChickenPhotoUrl(female != null ? female.getPhotoUrl() : null)
                .maleChickenId(male != null ? male.getId() : null)
                .maleChickenCode(male != null ? male.getChickenCode() : null)
                .maleChickenName(male != null ? male.getName() : null)
                .maleChickenBreed(male != null && male.getBreed() != null ? male.getBreed().name() : null)
                .breedingPairId(pair != null ? pair.getId() : null)
                .pairCode(pair != null ? pair.getPairCode() : null)
                .pairingDate(col.getPairingDate())
                .daysSincePairing(Math.max(0, daysSince))
                .eggLayingStartedDate(col.getEggLayingStartedDate())
                .currentBatchNumber(col.getCurrentBatchNumber())
                .batchCode(batchCode)
                .todayEggCount(col.getTodayEggCount())
                .totalEggCount(col.getTotalEggCount())
                .status(col.getStatus())
                .remarks(col.getRemarks())
                .build();
    }

    private EggItemResponse toEggItemResponse(EggItem item) {
        Chicken female = item.getFemaleChicken();
        Chicken male = item.getMaleChicken();
        String henCodeStr = female != null ? female.getChickenCode() : "H";
        String batchCode = String.format("EB-%s-%02d", henCodeStr, item.getBatchNumber());

        return EggItemResponse.builder()
                .id(item.getId())
                .eggCode(item.getEggCode())
                .femaleChickenId(female != null ? female.getId() : null)
                .femaleChickenCode(female != null ? female.getChickenCode() : null)
                .femaleChickenName(female != null ? female.getName() : null)
                .maleChickenId(male != null ? male.getId() : null)
                .maleChickenCode(male != null ? male.getChickenCode() : null)
                .maleChickenName(male != null ? male.getName() : null)
                .batchNumber(item.getBatchNumber())
                .batchCode(batchCode)
                .collectionDate(item.getCollectionDate())
                .status(item.getStatus())
                .purpose(item.getPurpose())
                .eggQuality(item.getEggQuality() != null ? item.getEggQuality().name() : "HEALTHY")
                .isMovedToHatching(item.getIsMovedToHatching())
                .remarks(item.getRemarks())
                .build();
    }

    private void recordTimelineEvent(Chicken chicken, String eventType, String title, String description) {
        if (chicken == null) return;
        ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                .chicken(chicken)
                .eventType(eventType)
                .title(title)
                .description(description)
                .createdBy("System")
                .build();
        chickenTimelineRepository.save(event);
    }

    private void createNotification(String title, String message, NotificationType type, Severity severity, Long refId) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .notificationType(type)
                .severity(severity)
                .sourceModule(SourceModule.BREEDING)
                .recipientRole(RecipientRole.ALL)
                .referenceId(refId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }
}
