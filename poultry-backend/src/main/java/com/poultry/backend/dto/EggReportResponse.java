package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggReportResponse {
    private Long totalEggsProduced;
    private Long totalDamagedEggs;
    private Double damagedRate;
    private Long eggsSold;
    private Long eggsAvailable;
    private Double dailyAverage;

    private Map<String, Long> purposeDistribution;
    private Map<String, Long> statusDistribution;
    private Map<LocalDate, Long> productionTrend;
}
