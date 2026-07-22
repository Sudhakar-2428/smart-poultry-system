package com.poultry.backend.dto;

import com.poultry.backend.entity.AccountType;
import com.poultry.backend.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountRequest {

    @NotBlank(message = "Account code is required")
    @Size(min = 2, max = 50, message = "Account code must be between 2 and 50 characters")
    private String accountCode;

    @NotBlank(message = "Account name is required")
    @Size(min = 2, max = 100, message = "Account name must be between 2 and 100 characters")
    private String accountName;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Opening balance is required")
    private Double openingBalance;

    @NotNull(message = "Status is required")
    private Status status;

    private String description;
}
