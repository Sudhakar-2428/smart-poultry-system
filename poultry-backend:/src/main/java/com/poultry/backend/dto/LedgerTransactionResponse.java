package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.ReferenceType;
import com.poultry.backend.entity.TransactionType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransactionResponse {
    private Long id;
    private String transactionCode;
    private LocalDate transactionDate;
    private TransactionType transactionType;
    private Long ledgerAccountId;
    private String ledgerAccountName;
    private Long incomeCategoryId;
    private String incomeCategoryName;
    private Long expenseCategoryId;
    private String expenseCategoryName;
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private Double amount;
    private PaymentMethod paymentMethod;
    private String createdBy;
    private String remarks;
    private LocalDateTime createdAt;
}
