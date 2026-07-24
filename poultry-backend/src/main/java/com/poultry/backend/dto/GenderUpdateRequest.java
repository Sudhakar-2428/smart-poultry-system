package com.poultry.backend.dto;

import com.poultry.backend.entity.Gender;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenderUpdateRequest {
    @NotNull(message = "Gender is required")
    private Gender gender;
}
