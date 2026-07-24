package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.LedgerAccountRequest;
import com.poultry.backend.dto.LedgerAccountResponse;
import com.poultry.backend.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/ledger-accounts", "/ledger-accounts"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ledger Account Management", description = "Endpoints for managing ledger accounts and accounts types")
public class LedgerAccountController {

    private final FinanceService financeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create ledger account", description = "Registers a new financial account. Requires ADMIN.")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> createLedgerAccount(@Valid @RequestBody LedgerAccountRequest request) {
        log.info("REST request to create ledger account. Code: {}", request.getAccountCode());
        LedgerAccountResponse response = financeService.createLedgerAccount(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Ledger account created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get list of ledger accounts", description = "Retrieves all ledger accounts in the system. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<List<LedgerAccountResponse>>> getAllLedgerAccounts() {
        log.info("REST request to get all ledger accounts");
        List<LedgerAccountResponse> response = financeService.getAllLedgerAccounts();
        return ResponseEntity.ok(ApiResponse.success(response, "Ledger accounts retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get ledger account by ID", description = "Retrieves specific ledger account details by ID. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> getLedgerAccountById(@PathVariable Long id) {
        log.info("REST request to get ledger account ID: {}", id);
        LedgerAccountResponse response = financeService.getLedgerAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Ledger account retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update ledger account by ID", description = "Updates details of an existing ledger account. Requires ADMIN.")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> updateLedgerAccount(
            @PathVariable Long id,
            @Valid @RequestBody LedgerAccountRequest request) {
        log.info("REST request to update ledger account ID: {}", id);
        LedgerAccountResponse response = financeService.updateLedgerAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Ledger account updated successfully"));
    }
}
