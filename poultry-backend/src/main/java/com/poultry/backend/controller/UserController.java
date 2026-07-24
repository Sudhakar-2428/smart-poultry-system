package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.UserDto;
import com.poultry.backend.dto.UserStatusRequest;
import com.poultry.backend.dto.UserUpdateRequest;
import com.poultry.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/users", "/users"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management (Admin Only)", description = "Endpoints for administrators to manage user accounts, status, and permissions")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get list of all users", description = "Retrieve list containing profile metadata of all registered user records")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        log.info("REST request to fetch all users (Admin only)");
        List<UserDto> users = userService.getAllUsers();
        ApiResponse<List<UserDto>> response = ApiResponse.success(users, "User accounts retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user account details by ID", description = "Retrieve user account data for a specific user ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        log.info("REST request to get user details for ID: {} (Admin only)", id);
        UserDto userDto = userService.getUserById(id);
        ApiResponse<UserDto> response = ApiResponse.success(userDto, "User details retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details by ID", description = "Modify user details including full name, email, phone, and role values")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("REST request to update user details for ID: {} (Admin only)", id);
        UserDto updatedUser = userService.updateUser(id, request);
        ApiResponse<UserDto> response = ApiResponse.success(updatedUser, "User details updated successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle user active status by ID", description = "Activate or Deactivate user account access capabilities")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        log.info("REST request to update status for user ID: {} to active={} (Admin only)", id, request.getActive());
        UserDto updatedUser = userService.updateUserStatus(id, request.getActive());
        String statusMessage = Boolean.TRUE.equals(request.getActive()) 
                ? "User account activated successfully" 
                : "User account deactivated successfully";
        ApiResponse<UserDto> response = ApiResponse.success(updatedUser, statusMessage);
        return ResponseEntity.ok(response);
    }
}
