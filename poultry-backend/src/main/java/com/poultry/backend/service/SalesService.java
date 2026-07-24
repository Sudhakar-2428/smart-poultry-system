package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SalesService {

    // Customer CRM logic
    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    void deleteCustomer(Long id);

    Page<CustomerResponse> searchCustomers(CustomerStatus status, Pageable pageable);

    // Sales Orders operations
    SalesOrderResponse createSalesOrder(SalesOrderRequest request);

    SalesOrderResponse getSalesOrderById(Long id);

    SalesOrderResponse updateSalesOrder(Long id, SalesOrderRequest request);

    SalesOrderResponse updateOrderStatus(Long id, SalesOrderStatusRequest request);

    Page<SalesOrderSummaryResponse> searchSalesOrders(
            Long customerId,
            SaleType saleType,
            PaymentStatus paymentStatus,
            SalesOrderStatus orderStatus,
            LocalDate orderDate,
            Long chickenId,
            Long eggBatchId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    // Reports Integration hooks
    Double getTotalRevenue();

    Double getSalesByCustomer(Long customerId);

    Double getSalesByChicken(Long chickenId);

    Double getSalesByEggBatch(Long eggBatchId);

    Double getDailySales(LocalDate date);

    Double getMonthlySales(int year, int month);
}
