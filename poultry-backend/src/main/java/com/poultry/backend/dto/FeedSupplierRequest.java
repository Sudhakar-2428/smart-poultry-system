package com.poultry.backend.dto;

import com.poultry.backend.entity.SupplierStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedSupplierRequest {

    @NotNull(message = "Supplier code is required")
    @Size(min = 2, max = 50, message = "Supplier code length must be between 2 and 50 characters")
    private String supplierCode;

    @NotNull(message = "Supplier name is required")
    @Size(min = 2, max = 100, message = "Supplier name length must be between 2 and 100 characters")
    private String supplierName;

    private String contactPerson;

    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    @NotNull(message = "Supplier status is required")
    private SupplierStatus status;

    private String remarks;
}
