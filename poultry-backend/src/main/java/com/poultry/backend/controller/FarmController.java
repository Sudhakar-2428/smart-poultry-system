package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.service.FarmMemberService;
import com.poultry.backend.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/farms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Farm Management", description = "Endpoints for managing farms, owners, join requests, roles and status of farm members")
public class FarmController {

    private final FarmService farmService;
    private final FarmMemberService farmMemberService;

    @PostMapping
    @Operation(summary = "Create a new farm", description = "Initialize a new farm and assign the caller as the Primary Owner")
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @Valid @RequestBody FarmRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to create farm: {} by creator {}", request.getName(), userDetails.getUsername());
        FarmResponse responseData = farmService.createFarm(request, userDetails.getUsername());
        ApiResponse<FarmResponse> response = ApiResponse.success(responseData, "Farm created successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{farmId}/location")
    @Operation(summary = "Update farm location coordinates", description = "Update farm address, latitude, and longitude (Primary Owner or Manager only)")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarmLocation(
            @PathVariable Long farmId,
            @Valid @RequestBody com.poultry.backend.dto.FarmLocationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to update location for farm ID: {} by {}", farmId, userDetails.getUsername());
        FarmResponse responseData = farmService.updateFarmLocation(farmId, request, userDetails.getUsername());
        ApiResponse<FarmResponse> response = ApiResponse.success(responseData, "Farm location updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-farm")
    @Operation(summary = "Get current user's farms", description = "Retrieve list of all active approved farms where this user is registered")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getMyFarms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get active farms for user: {}", userDetails.getUsername());
        List<FarmResponse> responseData = farmService.getMyFarms(userDetails.getUsername());
        ApiResponse<List<FarmResponse>> response = ApiResponse.success(responseData, "Active farms retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{farmUniqueId}")
    @Operation(summary = "Get farm by Unique ID", description = "Retrieve detailed configuration of a farm by its unique code ID")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarmByUniqueId(
            @PathVariable String farmUniqueId) {
        log.info("REST request to read farm by unique ID: {}", farmUniqueId);
        FarmResponse responseData = farmService.getFarmByUniqueId(farmUniqueId);
        ApiResponse<FarmResponse> response = ApiResponse.success(responseData, "Farm details retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join-request")
    @Operation(summary = "Submit a request to join a farm", description = "Request access to join a farm using its random join code")
    public ResponseEntity<ApiResponse<FarmMemberResponse>> joinRequest(
            @Valid @RequestBody JoinRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to submit farm join application by user: {}", userDetails.getUsername());
        FarmMemberResponse responseData = farmMemberService.createJoinRequest(request, userDetails.getUsername());
        ApiResponse<FarmMemberResponse> response = ApiResponse.success(responseData, "Join request submitted successfully. Pending approval.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-members")
    @Operation(summary = "Get pending member requests", description = "List pending join requests for all farms managed by the current owner or co-owner")
    public ResponseEntity<ApiResponse<List<FarmMemberResponse>>> getPendingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to list pending requests for actor: {}", userDetails.getUsername());
        List<FarmMemberResponse> responseData = farmMemberService.getPendingRequests(userDetails.getUsername());
        ApiResponse<List<FarmMemberResponse>> response = ApiResponse.success(responseData, "Pending member requests retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/members/{memberId}/approve")
    @Operation(summary = "Approve pending farm member request", description = "Approve a user's pending request to join the farm (Primary Owner can approve anyone, Co-Owners can approve Workers only)")
    public ResponseEntity<ApiResponse<FarmMemberResponse>> approveMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to approve member registration ID: {} by {}", memberId, userDetails.getUsername());
        FarmMemberResponse responseData = farmMemberService.approveMember(memberId, userDetails.getUsername());
        ApiResponse<FarmMemberResponse> response = ApiResponse.success(responseData, "Member approved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/members/{memberId}/reject")
    @Operation(summary = "Reject pending farm member request", description = "Reject a user's pending request to join the farm")
    public ResponseEntity<ApiResponse<FarmMemberResponse>> rejectMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to reject member registration ID: {} by {}", memberId, userDetails.getUsername());
        FarmMemberResponse responseData = farmMemberService.rejectMember(memberId, userDetails.getUsername());
        ApiResponse<FarmMemberResponse> response = ApiResponse.success(responseData, "Member request rejected successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "Remove a farm member", description = "Revoke membership access from a user in the farm")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to delete/remove member relationship ID: {} by manager {}", memberId, userDetails.getUsername());
        farmMemberService.removeMember(memberId, userDetails.getUsername());
        ApiResponse<Void> response = ApiResponse.success(null, "Member removed from farm successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/members/{memberId}/role")
    @Operation(summary = "Modify a member's role in the farm", description = "Change a member's role levels (Can only be performed by the Primary Owner, transfers ownership if changing destination to PRIMARY_OWNER)")
    public ResponseEntity<ApiResponse<FarmMemberResponse>> changeMemberRole(
            @PathVariable Long memberId,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to update member role ID: {} to {} by {}", memberId, request.getRole(), userDetails.getUsername());
        FarmMemberResponse responseData = farmMemberService.changeMemberRole(memberId, request.getRole(), userDetails.getUsername());
        ApiResponse<FarmMemberResponse> response = ApiResponse.success(responseData, "Member role updated successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/regenerate-join-code")
    @Operation(summary = "Regenerate unique join code for a farm", description = "Change a farm's access code string (Primary Owner only)")
    public ResponseEntity<ApiResponse<String>> regenerateJoinCode(
            @RequestParam String farmUniqueId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to regenerate access code for farm unique ID: {} by {}", farmUniqueId, userDetails.getUsername());
        String newCode = farmService.regenerateJoinCode(farmUniqueId, userDetails.getUsername());
        ApiResponse<String> response = ApiResponse.success(newCode, "Join code regenerated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{farmUniqueId}/members")
    @Operation(summary = "Get all members of a farm", description = "List all approved members associated with the specified farm ID")
    public ResponseEntity<ApiResponse<List<FarmMemberResponse>>> getFarmMembers(
            @PathVariable String farmUniqueId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to list members of farm: {} for caller {}", farmUniqueId, userDetails.getUsername());
        List<FarmMemberResponse> responseData = farmMemberService.getFarmMembers(farmUniqueId, userDetails.getUsername());
        ApiResponse<List<FarmMemberResponse>> response = ApiResponse.success(responseData, "Farm members retrieved successfully");
        return ResponseEntity.ok(response);
    }
}
