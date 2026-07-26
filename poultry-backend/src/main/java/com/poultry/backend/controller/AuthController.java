package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.service.AuthService;
import com.poultry.backend.service.UserService;
import com.poultry.backend.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication Manager", description = "Endpoints for managing user authentication, registration, password changes, and active details")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final WorkerService workerService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Create a new user account profile in the system with roles (WORKER by default unless changed by ADMIN)")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("REST request to register new user: {}", registerRequest.getEmail());
        UserDto registeredUser = authService.register(registerRequest);
        ApiResponse<UserDto> response = ApiResponse.success(registeredUser, "User account successfully registered");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in to retrieve JWT access token", description = "Submit authentication credentials (email + password) to receive a Bearer JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("REST request to log in user: {}", loginRequest.getEmail());
        AuthResponse authResponse = authService.login(loginRequest);
        ApiResponse<AuthResponse> response = ApiResponse.success(authResponse, "Authentication successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user profile", description = "Retrieve profile information for the authenticated key supplied in authorization headers")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get profile for authenticated user: {}", userDetails.getUsername());
        UserDto userDto = userService.getCurrentUser(userDetails.getUsername());
        ApiResponse<UserDto> response = ApiResponse.success(userDto, "Active profile retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change authenticated user password", description = "Verify current password and encode new strong password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        log.info("REST request to change password for user: {}", userDetails.getUsername());
        userService.changePassword(userDetails.getUsername(), changePasswordRequest);
        ApiResponse<Void> response = ApiResponse.success(null, "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/owner")
    @Operation(summary = "Register a new Farm Owner", description = "Create owner user and queue verification")
    public ResponseEntity<ApiResponse<UserDto>> registerOwner(@Valid @RequestBody OwnerRegisterRequest request) {
        log.info("REST request to register farm owner: {}", request.getEmail());
        UserDto registered = authService.registerOwner(request);
        ApiResponse<UserDto> response = ApiResponse.success(registered, "Owner registered. Please verify your email.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/worker")
    @Operation(summary = "Register a new Worker/Family Member", description = "Create worker user and queue verification")
    public ResponseEntity<ApiResponse<UserDto>> registerWorker(@Valid @RequestBody WorkerRegisterRequest request) {
        log.info("REST request to register farm worker: {}", request.getEmail());
        UserDto registered = authService.registerWorker(request);
        ApiResponse<UserDto> response = ApiResponse.success(registered, "Worker registered. Please verify your email.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify Email and activate Farm (if Owner)", description = "Verify email code/token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        log.info("REST request to verify email for: {}", request.getEmail());
        authService.verifyEmail(request);
        ApiResponse<Void> response = ApiResponse.success(null, "Email verified successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join-farm")
    @Operation(summary = "Request joining a farm (Worker/Family Member)", description = "Submit join request using Farm Unique ID and Join Code")
    public ResponseEntity<ApiResponse<Void>> joinFarm(@Valid @RequestBody JoinFarmRequest request) {
        log.info("REST request to join farm for user: {}", request.getEmail());
        authService.joinFarm(request);
        ApiResponse<Void> response = ApiResponse.success(null, "Farm join request submitted successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join-farm-temp")
    @Operation(summary = "Join farm using temporary credentials", description = "Allows a worker to join a farm using Farm ID, Worker ID, and Temporary Password")
    public ResponseEntity<ApiResponse<AuthResponse>> joinFarmWithTempPassword(
            @Valid @RequestBody JoinFarmTempRequest request) {
        log.info("REST request for worker to join farm using temporary password");
        AuthResponse authResponse = workerService.joinFarmWithTempPassword(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(authResponse, "Successfully joined farm!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out active user session", description = "Acknowledge client logout action")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("REST request to logout user session");
        ApiResponse<Void> response = ApiResponse.success(null, "Logged out successfully.");
        return ResponseEntity.ok(response);
    }
}
