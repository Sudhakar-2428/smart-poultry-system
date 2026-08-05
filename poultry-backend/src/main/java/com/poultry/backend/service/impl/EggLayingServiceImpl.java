package com.poultry.backend.service.impl;

import com.poultry.backend.dto.EggCollectionDTOs.EggCollectionResponse;
import com.poultry.backend.dto.EggLayingDTOs.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.EggCollectionService;
import com.poultry.backend.service.EggLayingService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EggLayingServiceImpl implements EggLayingService {

    private final BreedingPairRepository breedingPairRepository;
    private final EggCollectionRepository eggCollectionRepository;
    private final EggCollectionService eggCollectionService;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public EggLayingDashboardStats getDashboardStats() {
        List<BreedingPair> activePairs = breedingPairRepository.findByStatus(PairStatus.ACTIVE);
        long archivedPairs = breedingPairRepository.count((root, query, cb) -> cb.equal(root.get("status"), PairStatus.COMPLETED));

        long waiting = 0;
        long ready = 0;
        LocalDate today = LocalDate.now();

        for (BreedingPair pair : activePairs) {
            if (pair.getEggLayingStartedAt() == null) {
                long days = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), today) : 0;
                if (days >= 3) {
                    ready++;
                } else {
                    waiting++;
                }
            }
        }

        return EggLayingDashboardStats.builder()
                .totalLayingHens(waiting + ready)
                .readyForEggCollection(ready)
                .waitingPeriodHens(waiting)
                .activePairings((long) activePairs.size())
                .archivedPairings(archivedPairs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggLayingItemResponse> getActiveLayingHens(Pageable pageable) {
        Specification<BreedingPair> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PairStatus.ACTIVE),
                cb.isNull(root.get("eggLayingStartedAt"))
        );

        Page<BreedingPair> page = breedingPairRepository.findAll(spec, pageable);
        LocalDate today = LocalDate.now();

        List<EggLayingItemResponse> responses = page.getContent().stream().map(pair -> {
            Chicken female = pair.getFemaleChicken();
            Chicken male = pair.getMaleChicken();
            long days = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), today) : 0;
            boolean isReady = days >= 3;

            int batchNum = eggCollectionRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("femaleChicken").get("id"), female != null ? female.getId() : -1L)
            ).size() + 1;

            return EggLayingItemResponse.builder()
                    .pairId(pair.getId())
                    .pairCode(pair.getPairCode())
                    .femaleChickenId(female != null ? female.getId() : null)
                    .femaleChickenCode(female != null ? female.getChickenCode() : null)
                    .femaleChickenName(female != null ? female.getName() : null)
                    .femaleChickenBreed(female != null && female.getBreed() != null ? female.getBreed().name() : "COUNTRY_CHICKEN")
                    .femaleChickenPhotoUrl(female != null ? female.getPhotoUrl() : null)
                    .maleChickenId(male != null ? male.getId() : null)
                    .maleChickenCode(male != null ? male.getChickenCode() : null)
                    .maleChickenName(male != null ? male.getName() : null)
                    .maleChickenBreed(male != null && male.getBreed() != null ? male.getBreed().name() : null)
                    .pairingDate(pair.getStartDate())
                    .daysSincePairing(Math.max(0, days))
                    .expectedEggLayingDate(pair.getExpectedEggLayingDate() != null ? pair.getExpectedEggLayingDate() : (pair.getStartDate() != null ? pair.getStartDate().plusDays(3) : today))
                    .currentStage(isReady ? "Ready For Egg Collection" : "Waiting")
                    .status(pair.getStatus())
                    .isReadyForCollection(isReady)
                    .currentBatchNumber(batchNum)
                    .build();
        }).toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggLayingHistoryResponse> getLayingHistory(Pageable pageable) {
        Specification<BreedingPair> spec = (root, query, cb) -> cb.isNotNull(root.get("eggLayingStartedAt"));

        Page<BreedingPair> page = breedingPairRepository.findAll(spec, pageable);

        List<EggLayingHistoryResponse> responses = page.getContent().stream().map(pair -> {
            Chicken female = pair.getFemaleChicken();
            Chicken male = pair.getMaleChicken();
            LocalDateTime started = pair.getEggLayingStartedAt();
            long duration = (pair.getStartDate() != null && started != null) ? ChronoUnit.DAYS.between(pair.getStartDate(), started.toLocalDate()) : 0;

            int batchNum = eggCollectionRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("femaleChicken").get("id"), female != null ? female.getId() : -1L)
            ).size();

            return EggLayingHistoryResponse.builder()
                    .pairId(pair.getId())
                    .pairCode(pair.getPairCode())
                    .femaleChickenId(female != null ? female.getId() : null)
                    .femaleChickenCode(female != null ? female.getChickenCode() : null)
                    .femaleChickenName(female != null ? female.getName() : null)
                    .maleChickenId(male != null ? male.getId() : null)
                    .maleChickenCode(male != null ? male.getChickenCode() : null)
                    .maleChickenName(male != null ? male.getName() : null)
                    .pairingDate(pair.getStartDate())
                    .eggLayingStartedDate(started)
                    .transferDate(started)
                    .durationDays(Math.max(0, duration))
                    .batchNumber(Math.max(1, batchNum))
                    .currentStatus("Transferred")
                    .build();
        }).toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public EggCollectionResponse startEggCollection(Long pairId) {
        log.info("Executing startEggCollection action for pair ID: {}", pairId);
        BreedingPair pair = breedingPairRepository.findById(pairId)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + pairId));

        Chicken female = pair.getFemaleChicken();
        if (female == null) {
            throw new ValidationException("Pair does not have an assigned female chicken.");
        }

        // Idempotent check: if collection already exists for this pair, return existing
        Optional<EggCollection> existingCol = eggCollectionRepository.findAll((root, query, cb) ->
                cb.equal(root.get("breedingPair").get("id"), pair.getId())
        ).stream().findFirst();

        EggCollectionResponse collectionResponse;
        if (existingCol.isPresent()) {
            log.info("EggCollection already exists for pair ID {}. Returning existing record.", pairId);
            collectionResponse = eggCollectionService.getCollectionById(existingCol.get().getId());
        } else {
            collectionResponse = eggCollectionService.createEggCollectionFromPairing(pair);
        }

        // Mark egg laying started timestamp on pair
        if (pair.getEggLayingStartedAt() == null) {
            pair.setEggLayingStartedAt(LocalDateTime.now());
            breedingPairRepository.save(pair);

            // Record Timeline event
            recordTimelineEvent(female, "EGG_COLLECTION_STARTED", "Egg Collection Started",
                    "Transferred Hen " + female.getChickenCode() + " to Egg Collection Module after " +
                    ChronoUnit.DAYS.between(pair.getStartDate(), LocalDate.now()) + " days of pairing.");

            // Create notification
            createNotification("Egg Collection Started",
                    "Egg collection initiated for Hen " + female.getChickenCode() + " (Pairing " + pair.getPairCode() + ").",
                    NotificationType.BREEDING, Severity.INFO, pair.getId());
        }

        return collectionResponse;
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
