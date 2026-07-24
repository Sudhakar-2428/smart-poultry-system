package com.poultry.backend.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poultry.backend.dto.ReverseGeocodeRequest;
import com.poultry.backend.dto.ReverseGeocodeResponse;
import com.poultry.backend.service.LocationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final RestTemplate restTemplate;

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse?format=json&lat={lat}&lon={lon}";
    private static final String BIGDATACLOUD_URL = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude={lat}&longitude={lon}&localityLanguage=en";

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        Double lat = request.getLatitude();
        Double lon = request.getLongitude();

        log.info("Processing reverse geocoding request for lat: {}, lon: {}", lat, lon);

        String address = tryNominatimGeocoding(lat, lon);
        if (address == null) {
            address = tryBigDataCloudGeocoding(lat, lon);
        }

        if (address == null || address.isBlank()) {
            address = String.format("Location (%.4f, %.4f)", lat, lon);
        }

        return ReverseGeocodeResponse.builder()
                .address(address)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private String tryNominatimGeocoding(Double lat, Double lon) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartPoultryManagement/1.0 (contact@smartpoultry.com)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse> response = restTemplate.exchange(
                    NOMINATIM_URL,
                    HttpMethod.GET,
                    entity,
                    NominatimResponse.class,
                    lat, lon
            );

            if (response.getBody() != null && response.getBody().getDisplayName() != null) {
                return response.getBody().getDisplayName();
            }
        } catch (Exception e) {
            log.warn("Nominatim reverse geocoding failed: {}", e.getMessage());
        }
        return null;
    }

    private String tryBigDataCloudGeocoding(Double lat, Double lon) {
        try {
            BigDataCloudResponse response = restTemplate.getForObject(BIGDATACLOUD_URL, BigDataCloudResponse.class, lat, lon);
            if (response != null) {
                StringBuilder sb = new StringBuilder();
                if (response.getLocality() != null && !response.getLocality().isBlank()) {
                    sb.append(response.getLocality()).append(", ");
                }
                if (response.getPrincipalSubdivision() != null && !response.getPrincipalSubdivision().isBlank()) {
                    sb.append(response.getPrincipalSubdivision()).append(", ");
                }
                if (response.getCountryName() != null) {
                    sb.append(response.getCountryName());
                }
                String result = sb.toString().replaceAll(", $", "");
                if (!result.isBlank()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("BigDataCloud reverse geocoding failed: {}", e.getMessage());
        }
        return null;
    }

    @Data
    private static class NominatimResponse {
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    private static class BigDataCloudResponse {
        private String locality;
        private String principalSubdivision;
        private String countryName;
    }
}
