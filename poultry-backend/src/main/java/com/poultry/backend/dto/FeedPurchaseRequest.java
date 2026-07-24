package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPurchaseRequest {

    @NotNull(message = "Purchase code is required")
    @Size(min = 2, max = 50, message = "Purchase code length must be between 2 and 50 characters")
    private String purchaseCode;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @NotNull(message = "Feed item ID is required")
    private Long feedItemId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    private Double unitPrice;

    private String invoiceNumber;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;

    private String remarks;
}
