package com.poultry.backend.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRemindersResponse {
    private List<HealthRecordResponse> vaccinationsDueNext7Days;
    private List<HealthRecordResponse> overdueVaccinations;
    private List<HealthRecordResponse> followUpAppointmentsDue;
}
