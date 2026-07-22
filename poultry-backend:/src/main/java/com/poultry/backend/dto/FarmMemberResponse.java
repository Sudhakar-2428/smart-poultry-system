package com.poultry.backend.dto;

import com.poultry.backend.entity.FarmRole;
import com.poultry.backend.entity.MembershipStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmMemberResponse {
    private Long id;
    private Long farmId;
    private String farmName;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private FarmRole role;
    private MembershipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
