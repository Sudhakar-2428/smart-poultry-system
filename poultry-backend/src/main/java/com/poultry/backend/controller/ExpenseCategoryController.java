package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ExpenseCategoryRequest;
import com.poultry.backend.dto.ExpenseCategoryResponse;
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
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expense Category Management", description = "Endpoints for managing manual/automatic expense categories")
public class ExpenseCategoryController {

    private final FinanceService financeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create expense category", description = "Registers a new category for tagging expenses. Requires ADMIN.")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> createExpenseCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        log.info("REST request to create expense category. Code: {}", request.getCategoryCode());
        ExpenseCategoryResponse response = financeService.createExpenseCategory(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Expense category created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get list of expense categories", description = "Retrieves all expense categories. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryResponse>>> getAllExpenseCategories() {
        log.info("REST request to get all expense categories");
        List<ExpenseCategoryResponse> response = financeService.getAllExpenseCategories();
        return ResponseEntity.ok(ApiResponse.success(response, "Expense categories retrieved successfully"));
    }
}
