package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.ReferenceType;
import com.poultry.backend.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinanceService {

    // --- Ledger Accounts ---
    LedgerAccountResponse createLedgerAccount(LedgerAccountRequest request);
    LedgerAccountResponse updateLedgerAccount(Long id, LedgerAccountRequest request);
    LedgerAccountResponse getLedgerAccountById(Long id);
    List<LedgerAccountResponse> getAllLedgerAccounts();

    // --- Income Categories ---
    IncomeCategoryResponse createIncomeCategory(IncomeCategoryRequest request);
    List<IncomeCategoryResponse> getAllIncomeCategories();

    // --- Expense Categories ---
    ExpenseCategoryResponse createExpenseCategory(ExpenseCategoryRequest request);
    List<ExpenseCategoryResponse> getAllExpenseCategories();

    // --- Ledger Transactions ---
    LedgerTransactionResponse createManualTransaction(LedgerTransactionRequest request);
    LedgerTransactionResponse getLedgerTransactionById(Long id);
    Page<LedgerTransactionResponse> searchLedgerTransactions(
            TransactionType transactionType,
            ReferenceType referenceType,
            Long ledgerAccountId,
            Long incomeCategoryId,
            Long expenseCategoryId,
            PaymentMethod paymentMethod,
            LocalDate startDate,
            LocalDate endDate,
            Double minAmount,
            Double maxAmount,
            Pageable pageable
    );

    // --- Reporting Service Interfaces ---
    Double getTotalIncome();
    Double getTotalExpense();
    Double getNetProfit();
    Double getMonthlyIncome(int year, int month);
    Double getMonthlyExpense(int year, int month);
    Double getCashFlow(LocalDate startDate, LocalDate endDate);
    Map<String, Double> getAccountBalances();
    Map<String, Double> getExpenseByCategory();
    Map<String, Double> getIncomeByCategory();
}
