package com.poultry.backend.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poultry.backend.dto.FarmWeatherResponse;
import com.poultry.backend.dto.WeatherResponse;
import com.poultry.backend.entity.Farm;
import com.poultry.backend.exception.ExternalApiException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.service.WeatherService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final RestTemplate restTemplate;
    private final FarmRepository farmRepository;

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true";

    // In-memory SWR (Stale-While-Revalidate) Cache Store per farmId
    private final Map<Long, FarmWeatherResponse> farmWeatherCache = new ConcurrentHashMap<>();

    // Per-farm ReentrantLocks to prevent duplicate parallel Open-Meteo REST calls
    private final Map<Long, ReentrantLock> farmLocks = new ConcurrentHashMap<>();

    @Override
    @Cacheable(value = "weather", key = "T(java.lang.String).format('%.2f,%.2f', #lat, #lon)")
    public WeatherResponse getWeather(Double lat, Double lon) {
        long startTime = System.currentTimeMillis();
        log.info("[CACHE MISS] Fetching fresh live weather from Open-Meteo for lat: {}, lon: {}", lat, lon);
        try {
            OpenMeteoResponse response = restTemplate.getForObject(OPEN_METEO_URL, OpenMeteoResponse.class, lat, lon);
            long duration = System.currentTimeMillis() - startTime;

            if (response == null || response.getCurrentWeather() == null) {
                log.error("[WEATHER PROVIDER FAILURE] Open-Meteo returned no data for lat: {}, lon: {}", lat, lon);
                throw new ExternalApiException("Open-Meteo weather provider returned no data");
            }

            log.info("[WEATHER API TIME] Open-Meteo API Call Completed in {} ms for lat: {}, lon: {}", duration, lat, lon);

            CurrentWeather cw = response.getCurrentWeather();
            int temp = (int) Math.round(cw.getTemperature());
            int code = cw.getWeathercode();
            int isDay = cw.getIsDay();

            return WeatherResponse.builder()
                    .temperature(temp)
                    .condition(mapWeatherCodeToCondition(code, isDay))
                    .icon(mapWeatherCodeToIcon(code, isDay))
                    .build();

        } catch (RestClientException e) {
            log.error("[WEATHER PROVIDER FAILURE] RestClientException: {}", e.getMessage());
            throw new ExternalApiException("Failed to communicate with Open-Meteo weather service: " + e.getMessage(), e);
        }
    }

    @Override
    public FarmWeatherResponse getFarmWeather(Long farmId) {
        long startTime = System.currentTimeMillis();

        FarmWeatherResponse cached = farmWeatherCache.get(farmId);
        if (cached != null) {
            long minutesOld = Duration.between(cached.getLastUpdated(), LocalDateTime.now()).toMinutes();

            if (minutesOld < 15) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[CACHE HIT] Farm ID: {} | Returned fresh weather in {} ms | Temp: {}°C, Condition: {}", 
                        farmId, duration, cached.getTemperature(), cached.getCondition());
                return cloneResponseWithStaleFlag(cached, false);
            } else {
                log.info("[SWR STALE HIT] Farm ID: {} | Cache is {} mins old. Returning previous weather immediately & triggering async revalidation.", farmId, minutesOld);
                // Trigger async background revalidation without blocking caller thread
                CompletableFuture.runAsync(() -> refreshFarmWeather(farmId));
                return cloneResponseWithStaleFlag(cached, true);
            }
        }

        // Cache miss: Acquire lock for this specific farmId to prevent duplicate parallel Open-Meteo API calls
        log.info("[CACHE MISS] Farm ID: {} | Acquiring farm lock for initial fetch...", farmId);
        ReentrantLock lock = farmLocks.computeIfAbsent(farmId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-check cache inside lock
            cached = farmWeatherCache.get(farmId);
            if (cached != null) {
                log.info("[CACHE HIT POST-LOCK] Farm ID: {} | Using newly cached object from parallel thread", farmId);
                return cloneResponseWithStaleFlag(cached, false);
            }

            return fetchAndCacheFarmWeather(farmId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refreshFarmWeather(Long farmId) {
        ReentrantLock lock = farmLocks.computeIfAbsent(farmId, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.info("[DUPLICATE REFRESH SKIPPED] Refresh already in progress for Farm ID: {}", farmId);
            return;
        }

        try {
            fetchAndCacheFarmWeather(farmId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refreshAllFarmsWeather() {
        long startTime = System.currentTimeMillis();
        log.info("[SCHEDULER REFRESH START] Scanning active farms for background weather refresh...");

        List<Farm> activeFarms = farmRepository.findAll();
        int refreshedCount = 0;
        int skippedCount = 0;

        for (Farm farm : activeFarms) {
            if (farm.getLatitude() != null && farm.getLongitude() != null) {
                refreshFarmWeather(farm.getId());
                refreshedCount++;
            } else {
                skippedCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER REFRESH COMPLETE] Refreshed {} farm(s), skipped {} unconfigured farm(s) in {} ms",
                refreshedCount, skippedCount, duration);
    }

    private FarmWeatherResponse fetchAndCacheFarmWeather(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        Double lat = farm.getLatitude();
        Double lon = farm.getLongitude();

        if (lat == null || lon == null) {
            log.warn("Farm ID {} location coordinates not set in DB", farmId);
            FarmWeatherResponse fallback = FarmWeatherResponse.builder()
                    .farmId(farmId)
                    .farmName(farm.getName())
                    .temperature(null)
                    .condition("Location coordinates not set")
                    .icon("cloud-offline")
                    .lastUpdated(LocalDateTime.now())
                    .isStale(false)
                    .build();
            farmWeatherCache.put(farmId, fallback);
            return fallback;
        }

        long apiStartTime = System.currentTimeMillis();
        try {
            OpenMeteoResponse response = restTemplate.getForObject(OPEN_METEO_URL, OpenMeteoResponse.class, lat, lon);
            long apiDuration = System.currentTimeMillis() - apiStartTime;

            if (response == null || response.getCurrentWeather() == null) {
                log.error("[WEATHER PROVIDER FAILURE] Open-Meteo returned null/empty payload for Farm ID: {}", farmId);
                return handleProviderFailure(farmId, farm.getName());
            }

            log.info("[WEATHER API TIME] Farm ID: {} | Open-Meteo Call Duration: {} ms", farmId, apiDuration);

            CurrentWeather cw = response.getCurrentWeather();
            int temp = (int) Math.round(cw.getTemperature());
            int code = cw.getWeathercode();
            int isDay = cw.getIsDay();

            FarmWeatherResponse weather = FarmWeatherResponse.builder()
                    .farmId(farmId)
                    .farmName(farm.getName())
                    .temperature(temp)
                    .condition(mapWeatherCodeToCondition(code, isDay))
                    .icon(mapWeatherCodeToIcon(code, isDay))
                    .lastUpdated(LocalDateTime.now())
                    .isStale(false)
                    .build();

            farmWeatherCache.put(farmId, weather);
            return weather;

        } catch (Exception e) {
            log.error("[WEATHER PROVIDER FAILURE] Open-Meteo call failed for Farm ID {}: {}. Retaining previous cached state.", farmId, e.getMessage());
            return handleProviderFailure(farmId, farm.getName());
        }
    }

    private FarmWeatherResponse handleProviderFailure(Long farmId, String farmName) {
        FarmWeatherResponse previous = farmWeatherCache.get(farmId);
        if (previous != null) {
            log.info("[STALE CACHE RETAINED] Retaining previous valid weather state for Farm ID: {}", farmId);
            return cloneResponseWithStaleFlag(previous, true);
        }

        FarmWeatherResponse fallback = FarmWeatherResponse.builder()
                .farmId(farmId)
                .farmName(farmName)
                .temperature(null)
                .condition("Weather unavailable")
                .icon("cloud-offline")
                .lastUpdated(LocalDateTime.now())
                .isStale(true)
                .build();
        farmWeatherCache.put(farmId, fallback);
        return fallback;
    }

    private FarmWeatherResponse cloneResponseWithStaleFlag(FarmWeatherResponse src, boolean isStale) {
        return FarmWeatherResponse.builder()
                .farmId(src.getFarmId())
                .farmName(src.getFarmName())
                .temperature(src.getTemperature())
                .condition(src.getCondition())
                .icon(src.getIcon())
                .lastUpdated(src.getLastUpdated())
                .isStale(isStale)
                .build();
    }

    private String mapWeatherCodeToCondition(int code, int isDay) {
        switch (code) {
            case 0:
                return isDay == 1 ? "Sunny" : "Clear";
            case 1:
            case 2:
            case 3:
                return "Partly Cloudy";
            case 45:
            case 48:
                return "Foggy";
            case 51: case 53: case 55: case 56: case 57:
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82:
                return "Rainy";
            case 71: case 73: case 75: case 77:
            case 85: case 86:
                return "Snowy";
            case 95: case 96: case 99:
                return "Thunderstorm";
            default:
                return "Clear";
        }
    }

    private String mapWeatherCodeToIcon(int code, int isDay) {
        switch (code) {
            case 0:
                return isDay == 1 ? "sun" : "moon";
            case 1:
            case 2:
            case 3:
                return "cloud-sun";
            case 45:
            case 48:
                return "cloud-fog";
            case 51: case 53: case 55: case 56: case 57:
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82:
                return "cloud-rain";
            case 71: case 73: case 75: case 77:
            case 85: case 86:
                return "snowflake";
            case 95: case 96: case 99:
                return "cloud-lightning";
            default:
                return "sun";
        }
    }

    @Data
    private static class OpenMeteoResponse {
        @JsonProperty("current_weather")
        private CurrentWeather currentWeather;
    }

    @Data
    private static class CurrentWeather {
        private double temperature;
        private int weathercode;
        @JsonProperty("is_day")
        private int isDay;
    }
}
