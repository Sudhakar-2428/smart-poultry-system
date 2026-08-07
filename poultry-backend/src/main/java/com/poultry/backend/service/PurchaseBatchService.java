package com.poultry.backend.service;

import com.poultry.backend.dto.PurchaseBatchDTOs;

import java.util.List;

public interface PurchaseBatchService {
    PurchaseBatchDTOs.PurchaseBatchResponse createPurchaseBatch(PurchaseBatchDTOs.CreatePurchaseBatchRequest request);
    List<PurchaseBatchDTOs.PurchaseBatchResponse> getAllPurchaseBatches();
    PurchaseBatchDTOs.PurchaseBatchResponse getPurchaseBatchById(Long id);
    PurchaseBatchDTOs.PurchasedChickenReportDTO getPurchasedChickenReport(String reportType, String supplierName);
}
