package com.poultry.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmRequest {
    @NotBlank(message = "Farm name is required")
    private String name;

    private String farmAddress;
    private Double latitude;
    private Double longitude;
}
