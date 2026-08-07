package com.poultry.backend.scheduler;

import com.poultry.backend.service.EggNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EggNotificationScheduler {

    private final EggNotificationService eggNotificationService;

    // 08:00 AM Daily Notification Trigger
    @Scheduled(cron = "0 0 8 * * *")
    public void schedule08AMNotifications() {
        log.info("Cron Scheduler: Executing 08:00 AM Egg Collection Notification Job");
        eggNotificationService.triggerDaily08AMNotifications();
    }

    // 06:00 PM Escalation Job
    @Scheduled(cron = "0 0 18 * * *")
    public void schedule06PMEscalation() {
        log.info("Cron Scheduler: Executing 06:00 PM Egg Collection Worker Escalation Job");
        eggNotificationService.process06PMEscalation();
    }

    // 07:00 PM Manager Email Alert Job
    @Scheduled(cron = "0 0 19 * * *")
    public void schedule07PMEmailAlert() {
        log.info("Cron Scheduler: Executing 07:00 PM Manager Email Alert Escalation Job");
        eggNotificationService.process07PMManagerEmailAlert();
    }
}
