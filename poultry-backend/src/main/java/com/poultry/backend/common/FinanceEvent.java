package com.poultry.backend.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceEvent {
    private String eventType;
    private Long referenceId;
    private String referenceCode;
    private Double amount;
    private String description;
    private LocalDateTime timestamp;
}
