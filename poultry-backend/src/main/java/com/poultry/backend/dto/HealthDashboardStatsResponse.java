package com.poultry.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDashboardStatsResponse {
    private long healthy;
    private long underObservation;
    private long sick;
    private long recovered;
    private long critical;
    private long dead;
    private long upcomingVaccinationsNext30Days;
    private long overdueVaccinations;
    private long todayTreatments;
    private long vaccinationsDueNext7Days;
    private long followUpAppointmentsDue;
}
