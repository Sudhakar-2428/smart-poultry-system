package com.poultry.backend.scheduler;

import com.poultry.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FarmWeatherScheduler {

    private final WeatherService weatherService;

    /**
     * Cache Warmup on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[STARTUP CACHE WARMUP] Initializing farm weather cache pre-fetch...");
        try {
            weatherService.refreshAllFarmsWeather();
        } catch (Exception e) {
            log.warn("[STARTUP CACHE WARMUP WARNING] Non-blocking exception during startup warmup: {}", e.getMessage());
        }
    }

    /**
     * Background scheduled refresh every 15 minutes (900,000 milliseconds).
     */
    @Scheduled(fixedRate = 900000)
    public void scheduledWeatherRefresh() {
        log.info("[SCHEDULER 15-MIN REFRESH] Triggering periodic background weather update for active farms");
        try {
            weatherService.refreshAllFarmsWeather();
        } catch (Exception e) {
            log.error("[SCHEDULER REFRESH ERROR] Failed during scheduled weather refresh: {}", e.getMessage(), e);
        }
    }
}
