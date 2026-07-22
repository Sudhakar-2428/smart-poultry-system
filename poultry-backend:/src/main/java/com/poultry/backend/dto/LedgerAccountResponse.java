package com.poultry.backend.dto;

import com.poultry.backend.entity.AccountType;
import com.poultry.backend.entity.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountResponse {
    private Long id;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private Double openingBalance;
    private Double currentBalance;
    private String description;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
