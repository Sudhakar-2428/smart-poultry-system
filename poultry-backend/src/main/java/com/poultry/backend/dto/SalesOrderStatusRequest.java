package com.poultry.backend.dto;

import com.poultry.backend.entity.SalesOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderStatusRequest {
    @NotNull(message = "Order status is required")
    private SalesOrderStatus status;
    private String remarks;
}
