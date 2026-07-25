package com.poultry.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmProfileUpdateRequest {

    @NotBlank(message = "Farm name is required")
    @Size(max = 100, message = "Farm name cannot exceed 100 characters")
    private String farmName;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9\\-\\s()]{7,20}$", message = "Phone number format is invalid")
    private String phone;

    private String farmAddress;
    private String village;
    private String district;
    private String state;
    private String country;

    @Pattern(regexp = "^$|^[0-9]{3,10}$", message = "PIN code must contain numbers only")
    private String pinCode;

    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;
}
