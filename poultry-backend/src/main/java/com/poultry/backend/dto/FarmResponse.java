package com.poultry.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmResponse {
    private Long id;
    private String name;
    private String farmUniqueId;
    private String joinCode;
    private String farmAddress;
    private Double latitude;
    private Double longitude;
    private LocalDateTime locationLastUpdated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
