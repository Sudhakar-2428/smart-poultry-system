package com.poultry.backend.dto;

import com.poultry.backend.entity.CustomerStatus;
import com.poultry.backend.entity.CustomerType;
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
public class CustomerRequest {

    @NotNull(message = "Customer code is required")
    @Size(min = 2, max = 50, message = "Customer code length must be between 2 and 50 characters")
    private String customerCode;

    @NotNull(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name length must be between 2 and 100 characters")
    private String customerName;

    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @NotNull(message = "Customer status is required")
    private CustomerStatus status;

    private String remarks;
}
