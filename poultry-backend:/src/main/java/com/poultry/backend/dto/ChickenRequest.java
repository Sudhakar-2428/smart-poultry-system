package com.poultry.backend.dto;

import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenRequest {

    @NotBlank(message = "Chicken code is required")
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

    private Long motherId;

    private Long fatherId;

    private Long pairId;

    @Size(max = 255, message = "Photo URL cannot exceed 255 characters")
    private String photoUrl;

    private String remarks;
}
