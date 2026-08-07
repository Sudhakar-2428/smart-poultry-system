package com.poultry.backend.service.impl;

import com.poultry.backend.dto.EggCollectionQueueDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.EggCollectionQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EggCollectionQueueServiceImpl implements EggCollectionQueueService {

    private final EggCollectionQueueRepository eggCollectionQueueRepository;
    private final ChickenRepository chickenRepository;
    private final EggCollectionRepository eggCollectionRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;

    @Override
    @Transactional
    public EggCollectionQueueDTOs.EggQueueSummaryResponse getTodayQueue(String currentUser) {
        LocalDate today = LocalDate.now();
        List<EggCollectionQueueItem> items = eggCollectionQueueRepository.findByQueueDate(today);

        if (items.isEmpty()) {
            return generateTodayQueue();
        }

        return buildSummaryResponse(today, items);
    }

    @Override
    @Transactional
    public EggCollectionQueueDTOs.EggQueueSummaryResponse generateTodayQueue() {
        LocalDate today = LocalDate.now();
        log.info("Generating Daily Egg Collection Queue for date: {}", today);

        List<Chicken> hens = chickenRepository.findAll().stream()
                .filter(c -> c.getGender() == Gender.FEMALE && c.getStatus() == ChickenStatus.ACTIVE)
                .toList();

        for (Chicken hen : hens) {
            if (eggCollectionQueueRepository.findByQueueDateAndChickenId(today, hen.getId()).isEmpty()) {
                String breedStr = hen.getBreed() != null ? hen.getBreed().name() : "Country";
                EggCollectionQueueItem item = EggCollectionQueueItem.builder()
                        .queueDate(today)
                        .chickenId(hen.getId())
                        .henCode(hen.getChickenCode())
                        .henName(hen.getName() != null ? hen.getName() : "Hen " + hen.getChickenCode())
                        .breed(breedStr)
                        .photoUrl(hen.getPhotoUrl())
                        .pairingCode("PAIR-2026-001")
                        .eggLayingStartDate(today.minusDays(14))
                        .currentEggCount(12)
                        .status("PENDING")
                        .assignedWorkerEmail("worker@farm.com")
                        .build();
                eggCollectionQueueRepository.save(item);

                chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                        .chicken(hen)
                        .title("Added To Queue")
                        .description("Hen added to 08:00 AM Daily Egg Collection Queue for " + today)
                        .eventType("ADDED_TO_QUEUE")
                        .moduleName("EGG_COLLECTION")
                        .relatedEntityId(item.getId())
                        .createdBy("System")
                        .build());
            }
        }

        List<EggCollectionQueueItem> all = eggCollectionQueueRepository.findByQueueDate(today);
        return buildSummaryResponse(today, all);
    }

    @Override
    @Transactional
    public EggCollectionQueueDTOs.EggQueueItemResponse confirmQueueItem(Long itemId, EggCollectionQueueDTOs.ConfirmQueueItemRequest request, String currentUser) {
        EggCollectionQueueItem item = eggCollectionQueueRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Queue item not found with ID: " + itemId));

        item.setStatus("COMPLETED");
        item.setHealthyEggs(request.getHealthyEggs());
        item.setBrokenEggs(request.getBrokenEggs() != null ? request.getBrokenEggs() : 0);
        item.setDamagedEggs(request.getDamagedEggs() != null ? request.getDamagedEggs() : 0);
        item.setRemarks(request.getRemarks());
        item.setCompletedAt(LocalDateTime.now());

        EggCollectionQueueItem saved = eggCollectionQueueRepository.save(item);

        Chicken hen = chickenRepository.findById(item.getChickenId()).orElse(null);
        if (hen != null) {
            EggCollection collection = EggCollection.builder()
                    .femaleChicken(hen)
                    .eggLayingStartedDate(item.getQueueDate())
                    .todayEggCount(request.getHealthyEggs())
                    .totalEggCount(request.getHealthyEggs())
                    .status(EggCollectionStatus.ACTIVE)
                    .remarks(request.getRemarks())
                    .build();
            eggCollectionRepository.save(collection);

            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("Egg Collection Completed")
                    .description("Confirmed collection of " + request.getHealthyEggs() + " healthy eggs from daily queue.")
                    .eventType("EGG_COLLECTION_COMPLETED")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "Worker")
                    .build());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public EggCollectionQueueDTOs.EggQueueItemResponse noEggQueueItem(Long itemId, EggCollectionQueueDTOs.NoEggQueueItemRequest request, String currentUser) {
        EggCollectionQueueItem item = eggCollectionQueueRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Queue item not found with ID: " + itemId));

        item.setStatus("COMPLETED");
        item.setNoEggReason(request.getReason());
        item.setRemarks(request.getRemarks());
        item.setCompletedAt(LocalDateTime.now());

        EggCollectionQueueItem saved = eggCollectionQueueRepository.save(item);

        chickenRepository.findById(item.getChickenId()).ifPresent(hen -> {
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("Egg Collection Skipped")
                    .description("No egg laid today. Reason recorded: " + request.getReason())
                    .eventType("EGG_COLLECTION_SKIPPED")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "Worker")
                    .build());
        });

        return toResponse(saved);
    }

    @Override
    @Transactional
    public EggCollectionQueueDTOs.EggQueueItemResponse rescheduleQueueItem(Long itemId, EggCollectionQueueDTOs.RescheduleQueueItemRequest request, String currentUser) {
        EggCollectionQueueItem item = eggCollectionQueueRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Queue item not found with ID: " + itemId));

        item.setStatus("RESCHEDULED");
        item.setRescheduledUntil(LocalDateTime.now().plusMinutes(request.getDurationMinutes()));

        EggCollectionQueueItem saved = eggCollectionQueueRepository.save(item);

        chickenRepository.findById(item.getChickenId()).ifPresent(hen -> {
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("Reminder Rescheduled")
                    .description("Egg collection reminder rescheduled for " + request.getDurationMinutes() + " minutes.")
                    .eventType("REMINDER_RESCHEDULED")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "Worker")
                    .build());
        });

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EggCollectionQueueDTOs.EggQueueSummaryResponse getQueueReport(String status, String breed) {
        LocalDate today = LocalDate.now();
        List<EggCollectionQueueItem> items = eggCollectionQueueRepository.findByQueueDate(today).stream()
                .filter(i -> {
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        return i.getStatus().equalsIgnoreCase(status.trim());
                    }
                    return true;
                })
                .filter(i -> {
                    if (breed != null && !breed.trim().isEmpty() && !"ALL".equalsIgnoreCase(breed)) {
                        return i.getBreed() != null && i.getBreed().equalsIgnoreCase(breed.trim());
                    }
                    return true;
                })
                .toList();

        return buildSummaryResponse(today, items);
    }

    private EggCollectionQueueDTOs.EggQueueSummaryResponse buildSummaryResponse(LocalDate today, List<EggCollectionQueueItem> items) {
        long total = items.size();
        long pending = items.stream().filter(i -> "PENDING".equalsIgnoreCase(i.getStatus())).count();
        long completed = items.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
        long rescheduled = items.stream().filter(i -> "RESCHEDULED".equalsIgnoreCase(i.getStatus())).count();
        long escalated = items.stream().filter(i -> "ESCALATED".equalsIgnoreCase(i.getStatus())).count();

        double rate = total > 0 ? ((double) completed / total) * 100.0 : 0.0;

        List<EggCollectionQueueDTOs.EggQueueItemResponse> sortedResponses = items.stream()
                .sorted(Comparator.comparingInt(this::getPriorityWeight))
                .map(this::toResponse)
                .toList();

        return EggCollectionQueueDTOs.EggQueueSummaryResponse.builder()
                .queueDate(today)
                .totalHens(total)
                .pendingCount(pending)
                .completedCount(completed)
                .rescheduledCount(rescheduled)
                .escalatedCount(escalated)
                .completionRatePercentage(Math.round(rate * 10.0) / 10.0)
                .items(sortedResponses)
                .build();
    }

    private int getPriorityWeight(EggCollectionQueueItem item) {
        return switch (item.getStatus().toUpperCase()) {
            case "PENDING" -> 1;
            case "ESCALATED" -> 2;
            case "RESCHEDULED" -> 3;
            case "COMPLETED" -> 4;
            default -> 5;
        };
    }

    private EggCollectionQueueDTOs.EggQueueItemResponse toResponse(EggCollectionQueueItem i) {
        boolean isRescheduledActive = i.getRescheduledUntil() != null && i.getRescheduledUntil().isAfter(LocalDateTime.now());
        return EggCollectionQueueDTOs.EggQueueItemResponse.builder()
                .id(i.getId())
                .queueDate(i.getQueueDate())
                .chickenId(i.getChickenId())
                .henCode(i.getHenCode())
                .henName(i.getHenName())
                .breed(i.getBreed())
                .photoUrl(i.getPhotoUrl())
                .pairingCode(i.getPairingCode())
                .eggLayingStartDate(i.getEggLayingStartDate())
                .currentEggCount(i.getCurrentEggCount())
                .status(i.getStatus())
                .noEggReason(i.getNoEggReason())
                .healthyEggs(i.getHealthyEggs())
                .brokenEggs(i.getBrokenEggs())
                .damagedEggs(i.getDamagedEggs())
                .remarks(i.getRemarks())
                .assignedWorkerEmail(i.getAssignedWorkerEmail())
                .rescheduledUntil(i.getRescheduledUntil())
                .isRescheduledActive(isRescheduledActive)
                .completedAt(i.getCompletedAt())
                .build();
    }
}
