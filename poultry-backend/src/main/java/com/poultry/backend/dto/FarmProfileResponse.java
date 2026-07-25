package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmProfileResponse {
    private Long farmId;
    private String farmUniqueId;
    private String farmName;
    private String logoUrl;
    private String ownerName;
    private String email;
    private String phone;
    private String farmAddress;
    private String village;
    private String district;
    private String state;
    private String country;
    private String pinCode;
    private Double latitude;
    private Double longitude;
    private long totalWorkers;
    private long totalChickens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
