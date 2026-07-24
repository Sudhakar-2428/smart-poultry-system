package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.stereotype.Component;

@Component
public class FinanceMapper {

    // --- LedgerAccount Mapping ---
    public LedgerAccount toEntity(LedgerAccountRequest request) {
        if (request == null) return null;
        return LedgerAccount.builder()
                .accountCode(request.getAccountCode())
                .accountName(request.getAccountName())
                .accountType(request.getAccountType())
                .openingBalance(request.getOpeningBalance())
                .currentBalance(request.getOpeningBalance()) // Default current balance to opening balance on create
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

    public LedgerAccountResponse toResponse(LedgerAccount account) {
        if (account == null) return null;
        return LedgerAccountResponse.builder()
                .id(account.getId())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .openingBalance(account.getOpeningBalance())
                .currentBalance(account.getCurrentBalance())
                .description(account.getDescription())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    // --- IncomeCategory Mapping ---
    public IncomeCategory toEntity(IncomeCategoryRequest request) {
        if (request == null) return null;
        return IncomeCategory.builder()
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

    public IncomeCategoryResponse toResponse(IncomeCategory category) {
        if (category == null) return null;
        return IncomeCategoryResponse.builder()
                .id(category.getId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .status(category.getStatus())
                .build();
    }

    // --- ExpenseCategory Mapping ---
    public ExpenseCategory toEntity(ExpenseCategoryRequest request) {
        if (request == null) return null;
        return ExpenseCategory.builder()
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

    public ExpenseCategoryResponse toResponse(ExpenseCategory category) {
        if (category == null) return null;
        return ExpenseCategoryResponse.builder()
                .id(category.getId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .status(category.getStatus())
                .build();
    }

    // --- LedgerTransaction Mapping ---
    public LedgerTransactionResponse toResponse(LedgerTransaction transaction) {
        if (transaction == null) return null;

        Long ledgerAccountId = null;
        String ledgerAccountName = null;
        if (transaction.getLedgerAccount() != null) {
            ledgerAccountId = transaction.getLedgerAccount().getId();
            ledgerAccountName = transaction.getLedgerAccount().getAccountName();
        }

        Long incomeCategoryId = null;
        String incomeCategoryName = null;
        if (transaction.getIncomeCategory() != null) {
            incomeCategoryId = transaction.getIncomeCategory().getId();
            incomeCategoryName = transaction.getIncomeCategory().getCategoryName();
        }

        Long expenseCategoryId = null;
        String expenseCategoryName = null;
        if (transaction.getExpenseCategory() != null) {
            expenseCategoryId = transaction.getExpenseCategory().getId();
            expenseCategoryName = transaction.getExpenseCategory().getCategoryName();
        }

        return LedgerTransactionResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .ledgerAccountId(ledgerAccountId)
                .ledgerAccountName(ledgerAccountName)
                .incomeCategoryId(incomeCategoryId)
                .incomeCategoryName(incomeCategoryName)
                .expenseCategoryId(expenseCategoryId)
                .expenseCategoryName(expenseCategoryName)
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .createdBy(transaction.getCreatedBy())
                .remarks(transaction.getRemarks())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
