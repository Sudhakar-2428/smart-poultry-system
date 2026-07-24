package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class SalesMapper {

    public Customer toEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.builder()
                .customerCode(request.getCustomerCode())
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .customerType(request.getCustomerType())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .customerName(customer.getCustomerName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .customerType(customer.getCustomerType())
                .status(customer.getStatus())
                .remarks(customer.getRemarks())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public SalesOrder toEntity(SalesOrderRequest request) {
        if (request == null) {
            return null;
        }
        return SalesOrder.builder()
                .orderNumber(request.getOrderNumber())
                .orderDate(request.getOrderDate())
                .saleType(request.getSaleType())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(request.getPaymentStatus())
                .discount(request.getDiscount())
                .tax(request.getTax())
                .amountPaid(request.getAmountPaid())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .build();
    }

    public SalesOrderResponse toResponse(SalesOrder order) {
        if (order == null) {
            return null;
        }
        return SalesOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getCustomerName() : "")
                .orderDate(order.getOrderDate())
                .saleType(order.getSaleType())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .totalAmount(order.getTotalAmount())
                .amountPaid(order.getAmountPaid())
                .balanceAmount(order.getBalanceAmount())
                .status(order.getStatus())
                .remarks(order.getRemarks())
                .items(order.getItems() == null ? Collections.emptyList() :
                        order.getItems().stream().map(this::toResponse).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public SalesOrderSummaryResponse toSummaryResponse(SalesOrder order) {
        if (order == null) {
            return null;
        }
        return SalesOrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getCustomerName() : "")
                .orderDate(order.getOrderDate())
                .saleType(order.getSaleType())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .amountPaid(order.getAmountPaid())
                .balanceAmount(order.getBalanceAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public SalesOrderItem toEntity(SalesOrderItemRequest request) {
        if (request == null) {
            return null;
        }
        return SalesOrderItem.builder()
                .itemType(request.getItemType())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalPrice(request.getQuantity() * request.getUnitPrice())
                .remarks(request.getRemarks())
                .build();
    }

    public SalesOrderItemResponse toResponse(SalesOrderItem item) {
        if (item == null) {
            return null;
        }
        return SalesOrderItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .chickenId(item.getChicken() != null ? item.getChicken().getId() : null)
                .chickenCode(item.getChicken() != null ? item.getChicken().getChickenCode() : "")
                .eggBatchId(item.getEggBatch() != null ? item.getEggBatch().getId() : null)
                .eggBatchCode(item.getEggBatch() != null ? item.getEggBatch().getBatchCode() : "")
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .remarks(item.getRemarks())
                .build();
    }
}
