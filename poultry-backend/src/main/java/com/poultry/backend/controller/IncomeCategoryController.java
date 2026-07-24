package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.IncomeCategoryRequest;
import com.poultry.backend.dto.IncomeCategoryResponse;
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
@RequestMapping({"/api/v1/income-categories", "/income-categories"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Income Category Management", description = "Endpoints for managing manual/automatic income categories")
public class IncomeCategoryController {

    private final FinanceService financeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create income category", description = "Registers a new category for tagging income. Requires ADMIN.")
    public ResponseEntity<ApiResponse<IncomeCategoryResponse>> createIncomeCategory(@Valid @RequestBody IncomeCategoryRequest request) {
        log.info("REST request to create income category. Code: {}", request.getCategoryCode());
        IncomeCategoryResponse response = financeService.createIncomeCategory(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Income category created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get list of income categories", description = "Retrieves all income categories. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<List<IncomeCategoryResponse>>> getAllIncomeCategories() {
        log.info("REST request to get all income categories");
        List<IncomeCategoryResponse> response = financeService.getAllIncomeCategories();
        return ResponseEntity.ok(ApiResponse.success(response, "Income categories retrieved successfully"));
    }
}
