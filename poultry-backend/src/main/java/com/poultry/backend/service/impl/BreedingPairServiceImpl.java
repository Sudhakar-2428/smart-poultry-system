package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.BreedingPairMapper;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.BreedingPairService;
import com.poultry.backend.service.EggCollectionService;
import jakarta.persistence.criteria.Predicate;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class BreedingPairServiceImpl implements BreedingPairService {

    private final BreedingPairRepository breedingPairRepository;
    private final ChickenRepository chickenRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final EggRecordRepository eggRecordRepository;
    private final HatchResultRepository hatchResultRepository;
    private final NotificationRepository notificationRepository;
    private final EggCollectionService eggCollectionService;
    private final BreedingPairMapper pairMapper;

    private static final List<PairStatus> ACTIVE_PAIR_STATUSES = List.of(
            PairStatus.WAITING,
            PairStatus.READY_FOR_EGG_LAYING,
            PairStatus.TRANSFERRED,
            PairStatus.ACTIVE
    );

    @Override
    @Transactional
    public BreedingPairResponse createPair(BreedingPairRequest request) {
        log.info("Creating breeding pair. Code: {}", request.getPairCode());

        if (breedingPairRepository.existsByPairCode(request.getPairCode())) {
            throw new DuplicateRecordException("Breeding pair code '" + request.getPairCode() + "' is already registered.");
        }

        if (request.getMaleChickenId().equals(request.getFemaleChickenId())) {
            throw new ValidationException("Male and female chickens cannot be the same chicken.");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }

        Chicken male = chickenRepository.findById(request.getMaleChickenId())
                .orElseThrow(() -> new NotFoundException("Male chicken not found with ID: " + request.getMaleChickenId()));

        Chicken female = chickenRepository.findById(request.getFemaleChickenId())
                .orElseThrow(() -> new NotFoundException("Female chicken not found with ID: " + request.getFemaleChickenId()));

        // Bio-validations & Auto-category Assignment for Breeding
        if (male.getGender() != Gender.MALE) {
            throw new ValidationException("Male chicken gender must be MALE.");
        }
        if (male.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Male chicken status must be ACTIVE.");
        }
        if (male.getCategory() != ChickenCategory.ROOSTER && male.getCategory() != ChickenCategory.BREEDER) {
            male.setCategory(ChickenCategory.ROOSTER);
            chickenRepository.save(male);
        }

        if (female.getGender() != Gender.FEMALE) {
            throw new ValidationException("Female chicken gender must be FEMALE.");
        }
        if (female.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Female chicken status must be ACTIVE.");
        }
        if (female.getCategory() != ChickenCategory.LAYER && female.getCategory() != ChickenCategory.BREEDER) {
            female.setCategory(ChickenCategory.LAYER);
            chickenRepository.save(female);
        }

        // Active pair validations
        PairStatus targetStatus = PairStatus.ACTIVE;
        if (breedingPairRepository.existsByMaleChickenIdAndStatusIn(male.getId(), List.of(PairStatus.ACTIVE))) {
            throw new ValidationException("Male chicken is already assigned to an active pair.");
        }
        if (breedingPairRepository.existsByFemaleChickenIdAndStatusIn(female.getId(), List.of(PairStatus.ACTIVE))) {
            throw new ValidationException("Female chicken is already assigned to an active pair.");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate expectedLayingDate = startDate.plusDays(3);

        BreedingPair pair = pairMapper.toEntity(request);
        pair.setMaleChicken(male);
        pair.setFemaleChicken(female);
        pair.setStartDate(startDate);
        pair.setExpectedEggLayingDate(expectedLayingDate);
        pair.setStatus(targetStatus);
        if (request.getPairingType() != null) {
            pair.setPairingType(request.getPairingType());
        }

        BreedingPair saved = breedingPairRepository.save(pair);

        // Sync pairId on both chickens
        syncChickenPairIds(saved);

        // Record Timeline Events
        recordTimelineEvent(female, "PAIRING_CREATED", "Pairing Created",
                "Paired with Rooster " + male.getChickenCode() + " (" + (male.getName() != null ? male.getName() : "") + ") on " + startDate);
        recordTimelineEvent(male, "PAIRING_CREATED", "Pairing Created",
                "Paired with Hen " + female.getChickenCode() + " (" + (female.getName() != null ? female.getName() : "") + ") on " + startDate);

        // Create Notification
        createNotification("New Breeding Pair Created",
                "Hen " + female.getChickenCode() + " paired with Rooster " + male.getChickenCode() + ". Expected Egg Laying Date: " + expectedLayingDate,
                NotificationType.SYSTEM, Severity.INFO, saved.getId());

        log.info("AUDIT: Pair Created. ID: {}, Code: {}, Status: {}", saved.getId(), saved.getPairCode(), saved.getStatus());

        return pairMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BreedingPairResponse getPairById(Long id) {
        log.info("Retrieving breeding pair ID: {}", id);
        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));
        return pairMapper.toResponse(pair);
    }

    @Override
    @Transactional
    public BreedingPairResponse updatePair(Long id, BreedingPairRequest request) {
        log.info("Updating breeding pair ID: {}", id);

        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        if (breedingPairRepository.existsByPairCodeAndIdNot(request.getPairCode(), id)) {
            throw new DuplicateRecordException("Breeding pair code '" + request.getPairCode() + "' is already registered.");
        }

        if (request.getMaleChickenId().equals(request.getFemaleChickenId())) {
            throw new ValidationException("Male and female chickens cannot be the same chicken.");
        }

        Chicken male = chickenRepository.findById(request.getMaleChickenId())
                .orElseThrow(() -> new NotFoundException("Male chicken not found with ID: " + request.getMaleChickenId()));

        Chicken female = chickenRepository.findById(request.getFemaleChickenId())
                .orElseThrow(() -> new NotFoundException("Female chicken not found with ID: " + request.getFemaleChickenId()));

        if (male.getGender() != Gender.MALE || male.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Male chicken status must be ACTIVE.");
        }
        if (female.getGender() != Gender.FEMALE || female.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Female chicken status must be ACTIVE.");
        }

        if (ACTIVE_PAIR_STATUSES.contains(request.getStatus())) {
            if (breedingPairRepository.existsByMaleChickenIdAndStatusInAndIdNot(male.getId(), List.of(PairStatus.ACTIVE), id)) {
                throw new ValidationException("Male chicken is already assigned to an active pair.");
            }
            if (breedingPairRepository.existsByFemaleChickenIdAndStatusInAndIdNot(female.getId(), ACTIVE_PAIR_STATUSES, id)) {
                throw new ValidationException("Female chicken is already assigned to an active pair.");
            }
        }

        pair.setPairCode(request.getPairCode());
        pair.setMaleChicken(male);
        pair.setFemaleChicken(female);
        pair.setStartDate(request.getStartDate());
        pair.setEndDate(request.getEndDate());
        pair.setStatus(request.getStatus());
        pair.setBreedingPurpose(request.getBreedingPurpose());
        if (request.getPairingType() != null) pair.setPairingType(request.getPairingType());
        pair.setExpectedEggProduction(request.getExpectedEggProduction());
        pair.setRemarks(request.getRemarks());

        BreedingPair updated = breedingPairRepository.save(pair);
        syncChickenPairIds(updated);

        return pairMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public BreedingPairResponse updatePairStatus(Long id, PairStatusUpdateRequest request) {
        log.info("Patching breeding pair status for ID: {} to {}", id, request.getStatus());

        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        pair.setStatus(request.getStatus());
        if (request.getEndDate() != null) {
            pair.setEndDate(request.getEndDate());
        } else if (request.getStatus() == PairStatus.COMPLETED || request.getStatus() == PairStatus.CANCELLED || request.getStatus() == PairStatus.ARCHIVED) {
            pair.setEndDate(LocalDate.now());
        }

        if (request.getStatus() == PairStatus.ARCHIVED) {
            pair.setArchivedAt(LocalDateTime.now());
        }

        BreedingPair saved = breedingPairRepository.save(pair);
        syncChickenPairIds(saved);

        return pairMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BreedingPairResponse triggerEggLaying(Long pairId) {
        log.info("Triggering Egg Laying action for breeding pair ID: {}", pairId);

        BreedingPair pair = breedingPairRepository.findById(pairId)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + pairId));

        Chicken female = pair.getFemaleChicken();
        if (female == null) {
            throw new ValidationException("Pair does not have an assigned female chicken.");
        }

        // 1. Create Egg Record & Egg Collection Record
        EggRecord eggRecord = EggRecord.builder()
                .hen(female)
                .recordDate(LocalDate.now())
                .numberOfEggs(1)
                .damagedEggs(0)
                .remarks("Egg laying started from pairing code " + pair.getPairCode())
                .build();
        eggRecordRepository.save(eggRecord);

        // Auto-create Egg Collection record
        eggCollectionService.createEggCollectionFromPairing(pair);

        // 2. Update Pairing Status to TRANSFERRED and record timestamp
        pair.setStatus(PairStatus.TRANSFERRED);
        pair.setEggLayingStartedAt(LocalDateTime.now());
        BreedingPair savedPair = breedingPairRepository.save(pair);

        // 3. Record Timeline Events
        recordTimelineEvent(female, "EGG_LAYING_STARTED", "Moved to Egg Collection",
                "Hen moved automatically to Egg Collection module after pairing " + pair.getPairCode());
        if (pair.getMaleChicken() != null) {
            recordTimelineEvent(pair.getMaleChicken(), "EGG_LAYING_STARTED", "Hen Egg Laying Started",
                    "Partner Hen " + female.getChickenCode() + " initiated egg laying cycle.");
        }

        // 4. Trigger Notification
        createNotification("Hen Ready & Egg Collection Started",
                "Hen " + female.getChickenCode() + " (" + (female.getName() != null ? female.getName() : "Hen") + ") started egg laying and was automatically moved to Egg Collection.",
                NotificationType.SYSTEM, Severity.INFO, pair.getId());

        log.info("AUDIT: Egg Laying triggered. Pair ID: {}, Hen ID: {}", pairId, female.getId());

        return pairMapper.toResponse(savedPair);
    }

    @Override
    @Transactional
    public void deletePair(Long id) {
        log.info("Deleting breeding pair ID: {}", id);
        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        Chicken male = pair.getMaleChicken();
        Chicken female = pair.getFemaleChicken();
        if (male != null && id.equals(male.getPairId())) {
            male.setPairId(null);
            chickenRepository.save(male);
        }
        if (female != null && id.equals(female.getPairId())) {
            female.setPairId(null);
            chickenRepository.save(female);
        }

        breedingPairRepository.delete(pair);
    }

    @Override
    @Transactional
    public Page<BreedingPairSummaryResponse> searchPairs(
            Long maleChickenId,
            Long femaleChickenId,
            PairStatus status,
            BreedingPurpose purpose,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching breeding pairs with filters");

        Specification<BreedingPair> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (maleChickenId != null) {
                predicates.add(cb.equal(root.get("maleChicken").get("id"), maleChickenId));
            }
            if (femaleChickenId != null) {
                predicates.add(cb.equal(root.get("femaleChicken").get("id"), femaleChickenId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (purpose != null) {
                predicates.add(cb.equal(root.get("breedingPurpose"), purpose));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return breedingPairRepository.findAll(spec, pageable).map(pairMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PairingHistoryResponse> getPairingHistory(Pageable pageable) {
        log.info("Retrieving pairing history catalog");

        Specification<BreedingPair> spec = (root, query, cb) ->
                cb.or(
                        cb.equal(root.get("status"), PairStatus.ARCHIVED),
                        cb.equal(root.get("status"), PairStatus.COMPLETED),
                        cb.equal(root.get("status"), PairStatus.CANCELLED),
                        cb.equal(root.get("status"), PairStatus.TRANSFERRED)
                );

        Page<BreedingPair> page = breedingPairRepository.findAll(spec, pageable);
        List<PairingHistoryResponse> historyList = page.getContent().stream().map(pair -> {
            int eggsCount = (int) eggRecordRepository.sumTotalEggsByHenId(pair.getFemaleChicken().getId());
            int hatchBatches = (int) hatchResultRepository.count();
            int totalChicks = (int) hatchResultRepository.sumHatchedChicksInRange(LocalDate.of(2020, 1, 1), LocalDate.now().plusYears(1));
            return pairMapper.toHistoryResponse(pair, eggsCount, hatchBatches, totalChicks);
        }).toList();

        return new PageImpl<>(historyList, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public HenPairingProfileResponse getHenPairingProfile(Long henId) {
        log.info("Retrieving hen pairing profile details for hen ID: {}", henId);
        List<BreedingPair> activePairs = breedingPairRepository.findByFemaleChickenIdAndStatusIn(henId, ACTIVE_PAIR_STATUSES);

        if (activePairs.isEmpty()) {
            List<BreedingPair> allPairs = breedingPairRepository.findByFemaleChickenId(henId);
            if (allPairs.isEmpty()) return null;
            BreedingPair latest = allPairs.get(allPairs.size() - 1);
            return buildHenProfileResponse(latest, "Archived");
        }

        BreedingPair active = activePairs.get(0);
        String stage = active.getEggLayingStartedAt() != null ? "Egg Collection" : "Active";
        return buildHenProfileResponse(active, stage);
    }

    @Override
    @Transactional(readOnly = true)
    public RoosterPairingProfileResponse getRoosterPairingProfile(Long roosterId) {
        log.info("Retrieving rooster breeding profile for rooster ID: {}", roosterId);
        Chicken rooster = chickenRepository.findById(roosterId)
                .orElseThrow(() -> new NotFoundException("Rooster not found with ID: " + roosterId));

        List<BreedingPair> pairs = breedingPairRepository.findByMaleChickenId(roosterId);
        int totalPairings = pairs.size();
        int activePairings = (int) pairs.stream().filter(p -> p.getStatus() == PairStatus.ACTIVE).count();
        int completed = (int) pairs.stream().filter(p -> p.getStatus() == PairStatus.COMPLETED).count();

        List<RoosterPairingProfileResponse.LinkedHenSummary> linkedHens = pairs.stream().map(p -> {
            Chicken female = p.getFemaleChicken();
            return RoosterPairingProfileResponse.LinkedHenSummary.builder()
                    .henId(female != null ? female.getId() : null)
                    .henCode(female != null ? female.getChickenCode() : null)
                    .henName(female != null ? female.getName() : null)
                    .breed(female != null && female.getBreed() != null ? female.getBreed().name() : null)
                    .photoUrl(female != null ? female.getPhotoUrl() : null)
                    .pairingStatus(p.getStatus() != null ? p.getStatus().name() : "ACTIVE")
                    .pairingDate(p.getStartDate() != null ? p.getStartDate().toString() : "")
                    .build();
        }).toList();

        return RoosterPairingProfileResponse.builder()
                .roosterId(rooster.getId())
                .roosterCode(rooster.getChickenCode())
                .roosterName(rooster.getName())
                .totalPairings(totalPairings)
                .activePairings(activePairings)
                .completedPairings(completed)
                .totalFertileEggs(totalPairings * 12)
                .totalChicksProduced(totalPairings * 10)
                .linkedHens(linkedHens)
                .build();
    }

    @Override
    @Transactional
    public void checkAndUpdatePairingStatuses() {
        LocalDate today = LocalDate.now();

        // 1. Check WAITING -> READY_FOR_EGG_LAYING (after 3 days)
        List<BreedingPair> activePairs = breedingPairRepository.findByStatus(PairStatus.ACTIVE);
        for (BreedingPair pair : activePairs) {
            if (pair.getStartDate() != null && !pair.getStartDate().plusDays(3).isAfter(today) && pair.getEggLayingStartedAt() == null) {
                if (pair.getFemaleChicken() != null) {
                    recordTimelineEvent(pair.getFemaleChicken(), "PAIR_READY", "Ready for Egg Laying",
                            "Waiting period of 3 days completed. Hen is ready for Egg Laying.");
                }
            }
        }

        // 2. Auto-archive TRANSFERRED pairings after 2 days
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        for (BreedingPair pair : activePairs) {
            if (pair.getEggLayingStartedAt() != null && pair.getEggLayingStartedAt().isBefore(twoDaysAgo)) {
                pair.setStatus(PairStatus.COMPLETED);
                pair.setArchivedAt(LocalDateTime.now());
                if (pair.getEndDate() == null) pair.setEndDate(today);

                BreedingPair saved = breedingPairRepository.save(pair);

                // Clear chicken pairId mapping
                if (pair.getMaleChicken() != null && pair.getId().equals(pair.getMaleChicken().getPairId())) {
                    pair.getMaleChicken().setPairId(null);
                    chickenRepository.save(pair.getMaleChicken());
                }
                if (pair.getFemaleChicken() != null && pair.getId().equals(pair.getFemaleChicken().getPairId())) {
                    pair.getFemaleChicken().setPairId(null);
                    chickenRepository.save(pair.getFemaleChicken());
                }
                if (pair.getFemaleChicken() != null) {
                    recordTimelineEvent(pair.getFemaleChicken(), "PAIRING_ARCHIVED", "Pairing Archived",
                            "Breeding cycle completed and archived automatically.");
                    createNotification("Pairing Archived Successfully",
                            "Pairing " + pair.getPairCode() + " has been completed and archived automatically.",
                            NotificationType.SYSTEM, Severity.INFO, pair.getId());
                }
                log.info("AUTOMATION: Pair ID {} auto-archived TRANSFERRED -> ARCHIVED", pair.getId());
            }
        }
    }

    private HenPairingProfileResponse buildHenProfileResponse(BreedingPair pair, String stage) {
        Chicken rooster = pair.getMaleChicken();
        LocalDate now = LocalDate.now();
        long daysSince = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), now) : 0;

        return HenPairingProfileResponse.builder()
                .pairId(pair.getId())
                .pairCode(pair.getPairCode())
                .roosterId(rooster != null ? rooster.getId() : null)
                .roosterCode(rooster != null ? rooster.getChickenCode() : "")
                .roosterName(rooster != null ? rooster.getName() : "")
                .roosterBreed(rooster != null && rooster.getBreed() != null ? rooster.getBreed().name() : "")
                .roosterPhotoUrl(rooster != null ? rooster.getPhotoUrl() : null)
                .pairingDate(pair.getStartDate())
                .daysSincePairing(Math.max(0, daysSince))
                .currentStage(stage)
                .status(pair.getStatus())
                .build();
    }

    private void syncChickenPairIds(BreedingPair pair) {
        Chicken male = pair.getMaleChicken();
        Chicken female = pair.getFemaleChicken();

        if (ACTIVE_PAIR_STATUSES.contains(pair.getStatus())) {
            if (male != null) male.setPairId(pair.getId());
            if (female != null) female.setPairId(pair.getId());
        } else {
            if (male != null && pair.getId().equals(male.getPairId())) male.setPairId(null);
            if (female != null && pair.getId().equals(female.getPairId())) female.setPairId(null);
        }

        if (male != null) chickenRepository.save(male);
        if (female != null) chickenRepository.save(female);
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
                .sourceModule(SourceModule.SYSTEM)
                .recipientRole(RecipientRole.ALL)
                .referenceType("BREEDING_PAIR")
                .referenceId(refId)
                .build();
        notificationRepository.save(notification);
    }
}
