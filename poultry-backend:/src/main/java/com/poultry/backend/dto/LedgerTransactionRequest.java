package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransactionRequest {

    private String transactionCode;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Ledger account ID is required")
    private Long ledgerAccountId;

    private Long targetLedgerAccountId; // Required only for TRANSFER type

    private Long incomeCategoryId; // Optional, typically for INCOME type

    private Long expenseCategoryId; // Optional, typically for EXPENSE type

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String description;
    
    private String remarks;
}
