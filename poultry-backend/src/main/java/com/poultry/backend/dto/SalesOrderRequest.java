package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.PaymentStatus;
import com.poultry.backend.entity.SaleType;
import com.poultry.backend.entity.SalesOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderRequest {

    @NotNull(message = "Order number is required")
    @Size(min = 2, max = 50, message = "Order number length must be between 2 and 50 characters")
    private String orderNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @NotNull(message = "Sale type is required")
    private SaleType saleType;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;

    @Builder.Default
    @PositiveOrZero(message = "Discount must be positive or zero")
    private Double discount = 0.0;

    @Builder.Default
    @PositiveOrZero(message = "Tax must be positive or zero")
    private Double tax = 0.0;

    @Builder.Default
    @PositiveOrZero(message = "Amount paid must be positive or zero")
    private Double amountPaid = 0.0;

    @NotNull(message = "Order status is required")
    private SalesOrderStatus status;

    private String remarks;

    @NotEmpty(message = "Sales order must contain at least one item")
    @Valid
    private List<SalesOrderItemRequest> items;
}
