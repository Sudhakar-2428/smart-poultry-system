package com.poultry.backend.controller;

import com.poultry.backend.dto.WeatherResponse;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/weather", "/api/weather", "/weather"})
@RequiredArgsConstructor
@Tag(name = "Weather API", description = "Endpoints for fetching live weather data from external meteorology providers")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping({"/api/weather", "/weather"})
    @Operation(summary = "Get live weather by latitude and longitude", description = "Fetch live temperature, condition, and icon for specified geographic coordinates")
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon) {

        log.info("REST request to fetch weather for lat: {}, lon: {}", lat, lon);

        // Validation: Coordinates required
        if (lat == null || lon == null) {
            throw new ValidationException("Both 'lat' (latitude) and 'lon' (longitude) parameters are required.");
        }

        // Validation: Latitude bounds (-90 to 90)
        if (lat < -90.0 || lat > 90.0) {
            throw new ValidationException("Invalid latitude value '" + lat + "'. Latitude must be between -90 and 90 degrees.");
        }

        // Validation: Longitude bounds (-180 to 180)
        if (lon < -180.0 || lon > 180.0) {
            throw new ValidationException("Invalid longitude value '" + lon + "'. Longitude must be between -180 and 180 degrees.");
        }

        WeatherResponse weather = weatherService.getWeather(lat, lon);
        return ResponseEntity.ok(weather);
    }

    @GetMapping({"/api/v1/farms/{farmId}/weather", "/farms/{farmId}/weather"})
    @Operation(summary = "Get farm-specific weather by Farm ID", description = "Retrieve cached 15-minute weather condition based on stored farm location coordinates")
    public ResponseEntity<com.poultry.backend.dto.FarmWeatherResponse> getFarmWeather(
            @PathVariable Long farmId) {
        log.info("REST request to fetch farm weather for farm ID: {}", farmId);
        com.poultry.backend.dto.FarmWeatherResponse weather = weatherService.getFarmWeather(farmId);
        return ResponseEntity.ok(weather);
    }
}
