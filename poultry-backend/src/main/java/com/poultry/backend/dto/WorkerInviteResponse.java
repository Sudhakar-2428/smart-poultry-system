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
public class WorkerInviteResponse {
    private Long id;
    private Long userId;
    private String workerId;
    private Long farmId;
    private String farmUniqueId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String temporaryPassword;
    private FarmRole role;
    private MembershipStatus status;
    private LocalDateTime createdAt;
}
