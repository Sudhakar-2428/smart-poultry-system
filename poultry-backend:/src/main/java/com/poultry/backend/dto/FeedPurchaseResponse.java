package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPurchaseResponse {
    private Long id;
    private String purchaseCode;
    private Long supplierId;
    private String supplierName;
    private LocalDate purchaseDate;
    private Long feedItemId;
    private String feedName;
    private Double quantity;
    private Double unitPrice;
    private Double totalAmount;
    private String invoiceNumber;
    private PaymentStatus paymentStatus;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
