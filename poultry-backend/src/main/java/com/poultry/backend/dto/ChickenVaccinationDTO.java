package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenVaccinationDTO {

    private String vaccineName;
    private LocalDate vaccinationDate;
    private LocalDate nextDueDate;
    private String notes;
}
