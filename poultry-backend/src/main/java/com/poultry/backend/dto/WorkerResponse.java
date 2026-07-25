package com.poultry.backend.dto;

import com.poultry.backend.entity.FarmRole;
import com.poultry.backend.entity.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResponse {
    private Long id; // FarmMember ID
    private Long workerId; // User ID / Worker ID
    private Long farmId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private FarmRole role;
    private MembershipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
