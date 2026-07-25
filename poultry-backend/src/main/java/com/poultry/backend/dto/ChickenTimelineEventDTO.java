package com.poultry.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenTimelineEventDTO {
    private Long id;
    private String eventType;
    private String title;
    private String description;
    private String createdBy;
    private LocalDateTime timestamp;
}
