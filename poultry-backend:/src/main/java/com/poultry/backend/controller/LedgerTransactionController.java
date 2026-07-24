package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.LedgerTransactionRequest;
import com.poultry.backend.dto.LedgerTransactionResponse;
import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.ReferenceType;
import com.poultry.backend.entity.TransactionType;
import com.poultry.backend.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping({"/api/v1/ledger-transactions", "/ledger-transactions"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ledger Transaction Management", description = "Endpoints for managing manual ledger transactions and ledger searching")
public class LedgerTransactionController {

    private final FinanceService financeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create manual ledger transaction", description = "Allows ADMIN user to manually record Income, Expense or Transfer transactions. Requires ADMIN.")
    public ResponseEntity<ApiResponse<LedgerTransactionResponse>> createManualTransaction(@Valid @RequestBody LedgerTransactionRequest request) {
        log.info("REST request to record manual transaction. Type: {}, Amount: {}", request.getTransactionType(), request.getAmount());
        LedgerTransactionResponse response = financeService.createManualTransaction(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Manual transaction created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get transaction by ID", description = "Retrieves specific ledger transaction detail by ID. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<LedgerTransactionResponse>> getLedgerTransactionById(@PathVariable Long id) {
        log.info("REST request to view transaction ID: {}", id);
        LedgerTransactionResponse response = financeService.getLedgerTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List and search ledger transactions", description = "Queries ledger transactions filtered by type, references, dates, amounts, etc. with sorting and pagination. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<Page<LedgerTransactionResponse>>> searchLedgerTransactions(
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long ledgerAccountId,
            @RequestParam(required = false) Long incomeCategoryId,
            @RequestParam(required = false) Long expenseCategoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search ledger transactions");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<LedgerTransactionResponse> results = financeService.searchLedgerTransactions(
                transactionType, referenceType, ledgerAccountId, incomeCategoryId, expenseCategoryId, paymentMethod, startDate, endDate, minAmount, maxAmount, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Ledger transactions search completed successfully"));
    }
}
