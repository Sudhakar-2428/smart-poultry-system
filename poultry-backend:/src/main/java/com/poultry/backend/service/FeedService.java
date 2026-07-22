package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface FeedService {

    // Feed Items
    FeedItemResponse createFeedItem(FeedItemRequest request);

    FeedItemResponse getFeedItemById(Long id);

    FeedItemResponse updateFeedItem(Long id, FeedItemRequest request);

    void deleteFeedItem(Long id);

    Page<FeedItemResponse> searchFeedItems(FeedType type, FeedStatus status, LocalDate expiryDate, Pageable pageable);

    // Feed Suppliers
    FeedSupplierResponse registerSupplier(FeedSupplierRequest request);

    FeedSupplierResponse getSupplierById(Long id);

    FeedSupplierResponse updateSupplier(Long id, FeedSupplierRequest request);

    void deleteSupplier(Long id);

    Page<FeedSupplierResponse> searchSuppliers(SupplierStatus status, Pageable pageable);

    // Feed Purchases
    FeedPurchaseResponse recordFeedPurchase(FeedPurchaseRequest request);

    FeedPurchaseResponse getPurchaseById(Long id);

    Page<FeedPurchaseResponse> searchPurchases(Long supplierId, PaymentStatus paymentStatus, LocalDate purchaseDate, Pageable pageable);

    // Feed Consumption
    FeedConsumptionResponse recordFeedConsumption(FeedConsumptionRequest request);

    FeedConsumptionResponse getConsumptionById(Long id);

    Page<FeedConsumptionResponse> searchConsumptions(Long feedItemId, Long chickenId, Long brooderBatchId, LocalDate consumptionDate, Pageable pageable);

    // Future group feeding hook interface
    void recordGroupFeeding(Long feedItemId, List<Long> chickenIds, Double totalQuantity, LocalDate consumptionDate, String remarks);

    // Reporting Hooks
    Double getCurrentStock(Long feedItemId);

    List<FeedItemResponse> getLowStockItems();

    Double getMonthlyFeedConsumption(Long feedItemId, int year, int month);

    Double getFeedCost(Long feedItemId);

    Double getSupplierPurchaseSummary(Long supplierId);

    Double getFeedUsageByChicken(Long chickenId);

    Double getFeedUsageByBrooder(Long brooderBatchId);
}
