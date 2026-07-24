package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.stereotype.Component;

@Component
public class FeedMapper {

    public FeedItem toEntity(FeedItemRequest request) {
        if (request == null) {
            return null;
        }
        return FeedItem.builder()
                .feedCode(request.getFeedCode())
                .feedName(request.getFeedName())
                .feedType(request.getFeedType())
                .description(request.getDescription())
                .unit(request.getUnit())
                .minimumStock(request.getMinimumStock())
                .currentStock(request.getCurrentStock())
                .unitCost(request.getUnitCost())
                .storageLocation(request.getStorageLocation())
                .expiryDate(request.getExpiryDate())
                .status(request.getStatus())
                .build();
    }

    public FeedItemResponse toResponse(FeedItem item) {
        if (item == null) {
            return null;
        }
        return FeedItemResponse.builder()
                .id(item.getId())
                .feedCode(item.getFeedCode())
                .feedName(item.getFeedName())
                .feedType(item.getFeedType())
                .description(item.getDescription())
                .unit(item.getUnit())
                .minimumStock(item.getMinimumStock())
                .currentStock(item.getCurrentStock())
                .unitCost(item.getUnitCost())
                .storageLocation(item.getStorageLocation())
                .expiryDate(item.getExpiryDate())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public FeedSupplier toEntity(FeedSupplierRequest request) {
        if (request == null) {
            return null;
        }
        return FeedSupplier.builder()
                .supplierCode(request.getSupplierCode())
                .supplierName(request.getSupplierName())
                .contactPerson(request.getContactPerson())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .build();
    }

    public FeedSupplierResponse toResponse(FeedSupplier supplier) {
        if (supplier == null) {
            return null;
        }
        return FeedSupplierResponse.builder()
                .id(supplier.getId())
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getSupplierName())
                .contactPerson(supplier.getContactPerson())
                .phoneNumber(supplier.getPhoneNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .remarks(supplier.getRemarks())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    public FeedPurchase toEntity(FeedPurchaseRequest request) {
        if (request == null) {
            return null;
        }
        return FeedPurchase.builder()
                .purchaseCode(request.getPurchaseCode())
                .purchaseDate(request.getPurchaseDate())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(request.getQuantity() * request.getUnitPrice())
                .invoiceNumber(request.getInvoiceNumber())
                .paymentStatus(request.getPaymentStatus())
                .remarks(request.getRemarks())
                .build();
    }

    public FeedPurchaseResponse toResponse(FeedPurchase purchase) {
        if (purchase == null) {
            return null;
        }
        return FeedPurchaseResponse.builder()
                .id(purchase.getId())
                .purchaseCode(purchase.getPurchaseCode())
                .supplierId(purchase.getSupplier() != null ? purchase.getSupplier().getId() : null)
                .supplierName(purchase.getSupplier() != null ? purchase.getSupplier().getSupplierName() : "")
                .purchaseDate(purchase.getPurchaseDate())
                .feedItemId(purchase.getFeedItem() != null ? purchase.getFeedItem().getId() : null)
                .feedName(purchase.getFeedItem() != null ? purchase.getFeedItem().getFeedName() : "")
                .quantity(purchase.getQuantity())
                .unitPrice(purchase.getUnitPrice())
                .totalAmount(purchase.getTotalAmount())
                .invoiceNumber(purchase.getInvoiceNumber())
                .paymentStatus(purchase.getPaymentStatus())
                .remarks(purchase.getRemarks())
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .build();
    }

    public FeedConsumption toEntity(FeedConsumptionRequest request) {
        if (request == null) {
            return null;
        }
        return FeedConsumption.builder()
                .consumptionDate(request.getConsumptionDate())
                .quantity(request.getQuantity())
                .feedingType(request.getFeedingType())
                .remarks(request.getRemarks())
                .build();
    }

    public FeedConsumptionResponse toResponse(FeedConsumption consumption) {
        if (consumption == null) {
            return null;
        }
        return FeedConsumptionResponse.builder()
                .id(consumption.getId())
                .feedItemId(consumption.getFeedItem() != null ? consumption.getFeedItem().getId() : null)
                .feedName(consumption.getFeedItem() != null ? consumption.getFeedItem().getFeedName() : "")
                .chickenId(consumption.getChicken() != null ? consumption.getChicken().getId() : null)
                .chickenCode(consumption.getChicken() != null ? consumption.getChicken().getChickenCode() : "")
                .brooderBatchId(consumption.getBrooderBatch() != null ? consumption.getBrooderBatch().getId() : null)
                .brooderCode(consumption.getBrooderBatch() != null ? consumption.getBrooderBatch().getBrooderCode() : "")
                .consumptionDate(consumption.getConsumptionDate())
                .quantity(consumption.getQuantity())
                .feedingType(consumption.getFeedingType())
                .remarks(consumption.getRemarks())
                .createdAt(consumption.getCreatedAt())
                .updatedAt(consumption.getUpdatedAt())
                .build();
    }
}
