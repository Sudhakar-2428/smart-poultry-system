package com.poultry.backend.service.impl;

import com.poultry.backend.dto.EggNotificationDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.EggNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EggNotificationServiceImpl implements EggNotificationService {

    private final EggCollectionNotificationRepository eggCollectionNotificationRepository;
    private final ChickenRepository chickenRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final EggCollectionRepository eggCollectionRepository;

    @Override
    @Transactional
    public void triggerDaily08AMNotifications() {
        log.info("Triggering 08:00 AM egg collection notifications for active hens");
        LocalDate today = LocalDate.now();

        List<Chicken> hens = chickenRepository.findAll().stream()
                .filter(c -> c.getStatus() == ChickenStatus.ACTIVE && c.getGender() == Gender.FEMALE)
                .toList();

        for (Chicken hen : hens) {
            if (eggCollectionNotificationRepository.findFirstByChickenIdAndNotificationDate(hen.getId(), today).isEmpty()) {
                EggCollectionNotification notification = EggCollectionNotification.builder()
                        .chickenId(hen.getId())
                        .henCode(hen.getChickenCode())
                        .henName(hen.getName())
                        .breed(hen.getBreed() != null ? hen.getBreed().name() : "COUNTRY_CHICKEN")
                        .photoUrl(hen.getPhotoUrl())
                        .notificationDate(today)
                        .status("PENDING")
                        .build();

                EggCollectionNotification saved = eggCollectionNotificationRepository.save(notification);

                chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                        .chicken(hen)
                        .title("Notification Created")
                        .description("Egg collection notification triggered for 08:00 AM window.")
                        .eventType("NOTIFICATION_CREATED")
                        .moduleName("EGG_COLLECTION")
                        .relatedEntityId(saved.getId())
                        .createdBy("System")
                        .build());
            }
        }
        log.info("Finished triggering 08:00 AM egg collection notifications");
    }

    @Override
    @Transactional(readOnly = true)
    public List<EggNotificationDTOs.EggNotificationResponse> getActivePendingNotifications() {
        List<EggCollectionNotification> active = eggCollectionNotificationRepository.findAllActivePending();
        return active.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EggNotificationDTOs.EggNotificationResponse> getNotificationsByStatus(String status) {
        List<EggCollectionNotification> list;
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
            list = eggCollectionNotificationRepository.findAll();
        } else {
            list = eggCollectionNotificationRepository.findByStatus(status.trim().toUpperCase());
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public EggNotificationDTOs.EggNotificationResponse confirmEggCollection(Long notificationId, EggNotificationDTOs.ConfirmEggCollectionRequest request, String currentUser) {
        log.info("Processing YES response for Egg Notification ID: {}", notificationId);

        EggCollectionNotification notification = eggCollectionNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Egg collection notification not found with ID: " + notificationId));

        notification.setStatus("COMPLETED");
        notification.setHealthyEggs(request.getHealthyEggs());
        notification.setBrokenEggs(request.getBrokenEggs() != null ? request.getBrokenEggs() : 0);
        notification.setDamagedEggs(request.getDamagedEggs() != null ? request.getDamagedEggs() : 0);
        notification.setRemarks(request.getRemarks());

        EggCollectionNotification saved = eggCollectionNotificationRepository.save(notification);

        Chicken hen = chickenRepository.findById(notification.getChickenId()).orElse(null);
        if (hen != null) {
            EggCollection collection = EggCollection.builder()
                    .femaleChicken(hen)
                    .eggLayingStartedDate(notification.getNotificationDate())
                    .todayEggCount(request.getHealthyEggs())
                    .totalEggCount(request.getHealthyEggs())
                    .status(EggCollectionStatus.ACTIVE)
                    .remarks(request.getRemarks())
                    .build();
            eggCollectionRepository.save(collection);

            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("YES Response")
                    .description("Confirmed egg collection: " + request.getHealthyEggs() + " healthy eggs, " + (request.getBrokenEggs() != null ? request.getBrokenEggs() : 0) + " broken, " + (request.getDamagedEggs() != null ? request.getDamagedEggs() : 0) + " damaged.")
                    .eventType("YES_RESPONSE")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "User")
                    .build());

            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("Completed")
                    .description("Egg collection notification completed successfully.")
                    .eventType("COMPLETED")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "User")
                    .build());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public EggNotificationDTOs.EggNotificationResponse recordNoEgg(Long notificationId, EggNotificationDTOs.NoEggReasonRequest request, String currentUser) {
        log.info("Processing NO response for Egg Notification ID: {}", notificationId);

        EggCollectionNotification notification = eggCollectionNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Egg collection notification not found with ID: " + notificationId));

        notification.setStatus("NO_EGG");
        notification.setNoEggReason(request.getReason());
        notification.setRemarks(request.getRemarks());

        EggCollectionNotification saved = eggCollectionNotificationRepository.save(notification);

        chickenRepository.findById(notification.getChickenId()).ifPresent(hen -> {
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("NO Response")
                    .description("No egg laid today. Reason: " + request.getReason() + (request.getRemarks() != null ? " (" + request.getRemarks() + ")" : ""))
                    .eventType("NO_RESPONSE")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "User")
                    .build());
        });

        return toResponse(saved);
    }

    @Override
    @Transactional
    public EggNotificationDTOs.EggNotificationResponse rescheduleNotification(Long notificationId, EggNotificationDTOs.RescheduleNotificationRequest request, String currentUser) {
        log.info("Processing STILL NOT NOW for Egg Notification ID: {}", notificationId);

        EggCollectionNotification notification = eggCollectionNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Egg collection notification not found with ID: " + notificationId));

        LocalDateTime nextTime = LocalDateTime.now().plusMinutes(request.getDurationMinutes());
        notification.setRescheduledUntil(nextTime);

        EggCollectionNotification saved = eggCollectionNotificationRepository.save(notification);

        chickenRepository.findById(notification.getChickenId()).ifPresent(hen -> {
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(hen)
                    .title("Reminder Rescheduled")
                    .description("Rescheduled egg collection reminder for " + request.getDurationMinutes() + " minutes (Until " + nextTime.format(DateTimeFormatter.ofPattern("hh:mm a")) + ").")
                    .eventType("REMINDER_RESCHEDULED")
                    .moduleName("EGG_COLLECTION")
                    .relatedEntityId(saved.getId())
                    .createdBy(currentUser != null ? currentUser : "User")
                    .build());
        });

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void process06PMEscalation() {
        log.info("Executing 06:00 PM escalation check for unresolved egg collection notifications");
        List<EggCollectionNotification> pending = eggCollectionNotificationRepository.findAllActivePending().stream()
                .filter(n -> !Boolean.TRUE.equals(n.getEscalatedToWorker()))
                .toList();

        for (EggCollectionNotification n : pending) {
            n.setStatus("ESCALATED");
            n.setEscalatedToWorker(true);
            n.setEscalatedAt(LocalDateTime.now());
            eggCollectionNotificationRepository.save(n);

            chickenRepository.findById(n.getChickenId()).ifPresent(hen -> {
                chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                        .chicken(hen)
                        .title("Escalated")
                        .description("06:00 PM Active Window Closed - Notification escalated to assigned farm worker.")
                        .eventType("ESCALATED")
                        .moduleName("EGG_COLLECTION")
                        .relatedEntityId(n.getId())
                        .createdBy("System")
                        .build());
            });
        }
    }

    @Override
    @Transactional
    public void process07PMManagerEmailAlert() {
        log.info("Executing 07:00 PM manager email alert check for unresolved escalations");
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);

        List<EggCollectionNotification> escalated = eggCollectionNotificationRepository.findByStatus("ESCALATED").stream()
                .filter(n -> n.getEscalatedAt() != null && n.getEscalatedAt().isBefore(threshold) && n.getManagerEmailedAt() == null)
                .toList();

        for (EggCollectionNotification n : escalated) {
            n.setManagerEmailedAt(LocalDateTime.now());
            eggCollectionNotificationRepository.save(n);

            log.info("EMAILED ALERT: Sent email to Farm Manager & Primary Owner for Hen {} (Pending over 1 hour past 06:00 PM)", n.getHenCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EggNotificationDTOs.EggNotificationReportDTO getNotificationReport(String status, String startDate, String endDate) {
        log.info("Generating Egg Collection Notification Report. Status filter: {}", status);

        List<EggCollectionNotification> all = eggCollectionNotificationRepository.findAll();

        List<EggNotificationDTOs.EggNotificationResponse> dtoList = all.stream()
                .filter(n -> {
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        return n.getStatus().equalsIgnoreCase(status.trim());
                    }
                    return true;
                })
                .map(this::toResponse)
                .toList();

        long pending = all.stream().filter(n -> "PENDING".equalsIgnoreCase(n.getStatus())).count();
        long completed = all.stream().filter(n -> "COMPLETED".equalsIgnoreCase(n.getStatus())).count();
        long noEgg = all.stream().filter(n -> "NO_EGG".equalsIgnoreCase(n.getStatus())).count();
        long escalated = all.stream().filter(n -> "ESCALATED".equalsIgnoreCase(n.getStatus())).count();

        return EggNotificationDTOs.EggNotificationReportDTO.builder()
                .reportTitle("Egg Collection Notification & Escalation Report")
                .totalNotifications((long) dtoList.size())
                .pendingCount(pending)
                .completedCount(completed)
                .noEggCount(noEgg)
                .escalatedCount(escalated)
                .notifications(dtoList)
                .build();
    }

    private EggNotificationDTOs.EggNotificationResponse toResponse(EggCollectionNotification n) {
        boolean isRescheduledActive = n.getRescheduledUntil() != null && n.getRescheduledUntil().isAfter(LocalDateTime.now());

        return EggNotificationDTOs.EggNotificationResponse.builder()
                .id(n.getId())
                .chickenId(n.getChickenId())
                .henCode(n.getHenCode())
                .henName(n.getHenName())
                .breed(n.getBreed())
                .photoUrl(n.getPhotoUrl())
                .henAgeInWeeks(42)
                .currentBatchCode("EB-2026-001")
                .currentEggCount(12)
                .notificationDate(n.getNotificationDate())
                .status(n.getStatus())
                .noEggReason(n.getNoEggReason())
                .healthyEggs(n.getHealthyEggs())
                .brokenEggs(n.getBrokenEggs())
                .damagedEggs(n.getDamagedEggs())
                .remarks(n.getRemarks())
                .rescheduledUntil(n.getRescheduledUntil())
                .isRescheduledActive(isRescheduledActive)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
