package com.poultry.backend.dto;

import com.poultry.backend.entity.PaymentMethod;
import com.poultry.backend.entity.PaymentStatus;
import com.poultry.backend.entity.SaleType;
import com.poultry.backend.entity.SalesOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderResponse {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private LocalDate orderDate;
    private SaleType saleType;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double subtotal;
    private Double discount;
    private Double tax;
    private Double totalAmount;
    private Double amountPaid;
    private Double balanceAmount;
    private SalesOrderStatus status;
    private String remarks;
    private List<SalesOrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
