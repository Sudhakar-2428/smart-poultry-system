package com.poultry.backend.dto;

import com.poultry.backend.entity.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenRequest {

    @Size(max = 50, message = "Chicken code cannot exceed 50 characters")
    private String chickenCode;

    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Breed is required")
    private Breed breed;

    @NotNull(message = "Category is required")
    private ChickenCategory category;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @PositiveOrZero(message = "Weight must be zero or positive")
    private Double weight;

    @Size(max = 50, message = "Color cannot exceed 50 characters")
    private String color;

    @NotNull(message = "Status is required")
    private ChickenStatus status;

    private HealthStatus healthStatus;

    private ChickenOrigin origin;

    private LocalDate purchaseDate;

    @PositiveOrZero(message = "Purchase cost cannot be negative")
    private Double purchaseCost;

    @Size(max = 100, message = "Supplier name cannot exceed 100 characters")
    private String supplierName;

    @Size(max = 50, message = "Supplier contact cannot exceed 50 characters")
    private String supplierContact;

    @Size(max = 50, message = "Wing tag number cannot exceed 50 characters")
    private String wingTagNumber;

    @Size(max = 50, message = "Leg band number cannot exceed 50 characters")
    private String legBandNumber;

    private Boolean vaccinated;

    private List<ChickenVaccinationDTO> vaccinations;

    private Long motherId;

    private Long fatherId;

    private Long pairId;

    private String photoUrl;

    private String remarks;
}
