package com.poultry.backend.scheduler;

import com.poultry.backend.service.BreedingPairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PairingScheduler {

    private final BreedingPairService pairService;

    /**
     * Periodically check and transition pairing states automatically:
     * 1. WAITING -> READY_FOR_EGG_LAYING (after 3 days)
     * 2. TRANSFERRED -> ARCHIVED (after 2-3 days of transfer)
     * Runs every 10 minutes.
     */
    @Scheduled(fixedRate = 600000)
    public void processPairingLifecycleTransitions() {
        log.info("SCHEDULER: Running pairing lifecycle state transition check...");
        try {
            pairService.checkAndUpdatePairingStatuses();
        } catch (Exception e) {
            log.error("Error in PairingScheduler lifecycle check: {}", e.getMessage(), e);
        }
    }
}
