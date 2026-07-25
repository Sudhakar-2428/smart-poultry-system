package com.poultry.backend.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenVaccination {

    private String vaccineName;
    private LocalDate vaccinationDate;
    private LocalDate nextDueDate;
    private String notes;
}
