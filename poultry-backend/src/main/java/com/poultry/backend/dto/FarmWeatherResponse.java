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
public class FarmWeatherResponse {
    private Long farmId;
    private String farmName;
    private Integer temperature;
    private String condition;
    private String icon;
    private LocalDateTime lastUpdated;
    private Boolean isStale;
}
