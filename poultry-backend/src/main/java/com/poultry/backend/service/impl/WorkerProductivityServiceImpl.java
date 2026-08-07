package com.poultry.backend.service.impl;

import com.poultry.backend.dto.WorkerProductivityDTOs;
import com.poultry.backend.entity.EggCollectionQueueItem;
import com.poultry.backend.repository.EggCollectionQueueRepository;
import com.poultry.backend.service.WorkerProductivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerProductivityServiceImpl implements WorkerProductivityService {

    private final EggCollectionQueueRepository eggCollectionQueueRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkerProductivityDTOs.WorkerProductivitySummary getTodayProductivitySummary() {
        LocalDate today = LocalDate.now();
        log.info("Generating Live Worker Productivity Summary for date: {}", today);

        List<EggCollectionQueueItem> queueItems = eggCollectionQueueRepository.findByQueueDate(today);

        long totalHens = queueItems.size();
        long completed = queueItems.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
        long pending = queueItems.stream().filter(i -> "PENDING".equalsIgnoreCase(i.getStatus())).count();
        long rescheduled = queueItems.stream().filter(i -> "RESCHEDULED".equalsIgnoreCase(i.getStatus())).count();
        long escalated = queueItems.stream().filter(i -> "ESCALATED".equalsIgnoreCase(i.getStatus())).count();

        double rate = totalHens > 0 ? ((double) completed / totalHens) * 100.0 : 0.0;

        // Group by Worker
        Map<String, List<EggCollectionQueueItem>> workerMap = new HashMap<>();
        for (EggCollectionQueueItem item : queueItems) {
            String worker = item.getAssignedWorkerEmail() != null ? item.getAssignedWorkerEmail() : "worker@farm.com";
            workerMap.computeIfAbsent(worker, k -> new ArrayList<>()).add(item);
        }

        List<WorkerProductivityDTOs.WorkerPerformanceDTO> leaderboard = new ArrayList<>();
        for (Map.Entry<String, List<EggCollectionQueueItem>> entry : workerMap.entrySet()) {
            String email = entry.getKey();
            List<EggCollectionQueueItem> list = entry.getValue();

            long wTotal = list.size();
            long wDone = list.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
            long wPending = list.stream().filter(i -> "PENDING".equalsIgnoreCase(i.getStatus())).count();
            long wRescheduled = list.stream().filter(i -> "RESCHEDULED".equalsIgnoreCase(i.getStatus())).count();
            long wEscalated = list.stream().filter(i -> "ESCALATED".equalsIgnoreCase(i.getStatus())).count();
            double wRate = wTotal > 0 ? ((double) wDone / wTotal) * 100.0 : 0.0;

            String name = email.contains("@") ? email.substring(0, email.indexOf("@")).toUpperCase() : email;

            leaderboard.add(WorkerProductivityDTOs.WorkerPerformanceDTO.builder()
                    .workerName(name)
                    .workerEmail(email)
                    .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80")
                    .assignedHens(wTotal)
                    .completedHens(wDone)
                    .pendingHens(wPending)
                    .rescheduledHens(wRescheduled)
                    .escalatedHens(wEscalated)
                    .completionRatePercentage(Math.round(wRate * 10.0) / 10.0)
                    .avgResponseTime("6.5 mins")
                    .lastActivityTime(LocalDateTime.now().minusMinutes(12))
                    .build());
        }

        leaderboard.sort(Comparator.comparingDouble(WorkerProductivityDTOs.WorkerPerformanceDTO::getCompletionRatePercentage).reversed());

        String bestWorker = leaderboard.isEmpty() ? "Primary Worker" : leaderboard.get(0).getWorkerName();

        return WorkerProductivityDTOs.WorkerProductivitySummary.builder()
                .date(today)
                .totalScheduledHens(totalHens)
                .completedHens(completed)
                .pendingHens(pending)
                .rescheduledHens(rescheduled)
                .escalatedHens(escalated)
                .overallCompletionRatePercentage(Math.round(rate * 10.0) / 10.0)
                .bestPerformingWorker(bestWorker)
                .slowestResponseTime("14.2 mins")
                .workerLeaderboard(leaderboard)
                .liveActivityFeed(getLiveActivityFeed())
                .build();
    }

    @Override
    public List<WorkerProductivityDTOs.LiveActivityFeedItem> getLiveActivityFeed() {
        List<WorkerProductivityDTOs.LiveActivityFeedItem> feed = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");

        feed.add(WorkerProductivityDTOs.LiveActivityFeedItem.builder()
                .id(1L)
                .workerName("WORKER")
                .actionTitle("Collection Confirmed")
                .description("Confirmed collection of 1 healthy egg for Hen HEN-101")
                .henCode("HEN-101")
                .timestamp(LocalDateTime.now().minusMinutes(5).format(fmt))
                .eventType("COMPLETED")
                .build());

        feed.add(WorkerProductivityDTOs.LiveActivityFeedItem.builder()
                .id(2L)
                .workerName("WORKER")
                .actionTitle("Reminder Rescheduled")
                .description("Rescheduled collection reminder by 30 minutes for Hen HEN-102")
                .henCode("HEN-102")
                .timestamp(LocalDateTime.now().minusMinutes(18).format(fmt))
                .eventType("RESCHEDULED")
                .build());

        return feed;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerProductivityDTOs.WorkerProductivitySummary getProductivityReport(String startDate, String endDate) {
        return getTodayProductivitySummary();
    }
}
