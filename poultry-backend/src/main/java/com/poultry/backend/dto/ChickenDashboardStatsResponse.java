package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenDashboardStatsResponse {
    private long totalChickens;
    private long healthy;
    private long sick;
    private long sold;
    private long dead;
    private long hens;
    private long roosters;
    private long countryChickens;
    private long broilers;
    private long layers;
    private long recentlyRegistered;
}
