package com.poultry.backend.dto;

import com.poultry.backend.entity.Status;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryResponse {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Status status;
}
