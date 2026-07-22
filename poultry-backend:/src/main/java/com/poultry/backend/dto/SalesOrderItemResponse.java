package com.poultry.backend.dto;

import com.poultry.backend.entity.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderItemResponse {
    private Long id;
    private ItemType itemType;
    private Long chickenId;
    private String chickenCode;
    private Long eggBatchId;
    private String eggBatchCode;
    private Double quantity;
    private Double unitPrice;
    private Double totalPrice;
    private String remarks;
}
