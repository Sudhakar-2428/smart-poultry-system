package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DangerZoneResponse {

    private boolean success;
    private String message;
    private Map<String, Long> deletedRecords;
    private LocalDateTime executedAt;
}
