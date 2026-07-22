package com.poultry.backend.dto;

import com.poultry.backend.entity.ChickenCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdultTransitionRequest {
    @NotNull(message = "Category is required")
    private ChickenCategory category;

    private String remarks;
}
