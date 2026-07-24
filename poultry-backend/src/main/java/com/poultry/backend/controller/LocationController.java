package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ReverseGeocodeRequest;
import com.poultry.backend.dto.ReverseGeocodeResponse;
import com.poultry.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Location Service", description = "Endpoints for reverse geocoding coordinates to human-readable addresses")
public class LocationController {

    private final LocationService locationService;

    @PostMapping({"/api/v1/location/reverse-geocode", "/location/reverse-geocode"})
    @Operation(summary = "Reverse geocode latitude and longitude", description = "Convert GPS coordinates (latitude, longitude) into a human-readable street/city address")
    public ResponseEntity<ApiResponse<ReverseGeocodeResponse>> reverseGeocode(
            @Valid @RequestBody ReverseGeocodeRequest request) {
        log.info("REST request to reverse geocode lat: {}, lon: {}", request.getLatitude(), request.getLongitude());
        ReverseGeocodeResponse responseData = locationService.reverseGeocode(request);
        ApiResponse<ReverseGeocodeResponse> response = ApiResponse.success(responseData, "Address retrieved successfully");
        return ResponseEntity.ok(response);
    }
}
