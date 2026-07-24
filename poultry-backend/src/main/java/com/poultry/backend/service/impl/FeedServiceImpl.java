package com.poultry.backend.service.impl;

import com.poultry.backend.common.FinanceEvent;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.FeedMapper;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.FeedService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedItemRepository feedItemRepository;
    private final FeedSupplierRepository feedSupplierRepository;
    private final FeedPurchaseRepository feedPurchaseRepository;
    private final FeedConsumptionRepository feedConsumptionRepository;
    private final ChickenRepository chickenRepository;
    private final BrooderBatchRepository brooderBatchRepository;
    private final NotificationRepository notificationRepository;
    private final FeedMapper feedMapper;
    
    // Spring Event publisher for FinanceEvent hooks
    private final ApplicationEventPublisher eventPublisher;

    // --- Feed Items ---
    @Override
    @Transactional
    public FeedItemResponse createFeedItem(FeedItemRequest request) {
        log.info("Creating feed item. Code: {}", request.getFeedCode());

        if (feedItemRepository.existsByFeedCode(request.getFeedCode())) {
            throw new DuplicateRecordException("Feed code '" + request.getFeedCode() + "' is already registered.");
        }

        FeedItem item = feedMapper.toEntity(request);
        
        // Auto update status based on stock level and expiry
        adjustFeedStatusBasedOnStock(item);

        FeedItem saved = feedItemRepository.save(item);
        log.info("AUDIT: Feed Item Created. ID: {}, Code: {}", saved.getId(), saved.getFeedCode());

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedItemResponse getFeedItemById(Long id) {
        log.info("Retrieving feed item ID: {}", id);
        FeedItem item = feedItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feed item not found with ID: " + id));
        return feedMapper.toResponse(item);
    }

    @Override
    @Transactional
    public FeedItemResponse updateFeedItem(Long id, FeedItemRequest request) {
        log.info("Updating feed item ID: {}", id);

        FeedItem item = feedItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feed item not found with ID: " + id));

        if (feedItemRepository.existsByFeedCodeAndIdNot(request.getFeedCode(), id)) {
            throw new DuplicateRecordException("Feed code '" + request.getFeedCode() + "' is already registered.");
        }

        item.setFeedCode(request.getFeedCode());
        item.setFeedName(request.getFeedName());
        item.setFeedType(request.getFeedType());
        item.setDescription(request.getDescription());
        item.setUnit(request.getUnit());
        item.setMinimumStock(request.getMinimumStock());
        item.setCurrentStock(request.getCurrentStock());
        item.setUnitCost(request.getUnitCost());
        item.setStorageLocation(request.getStorageLocation());
        item.setExpiryDate(request.getExpiryDate());
        
        // Explicitly set request status first to allow updates to INACTIVE
        item.setStatus(request.getStatus());
        adjustFeedStatusBasedOnStock(item);

        FeedItem saved = feedItemRepository.save(item);
        log.info("AUDIT: Stock Updated. ID: {}, Code: {}, Current Stock: {}", saved.getId(), saved.getFeedCode(), saved.getCurrentStock());

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFeedItem(Long id) {
        log.info("Deleting feed item ID: {}", id);
        FeedItem item = feedItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feed item not found with ID: " + id));
        feedItemRepository.delete(item);
        log.info("AUDIT: Feed Item Deleted. Code: {}", item.getFeedCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedItemResponse> searchFeedItems(FeedType type, FeedStatus status, LocalDate expiryDate, Pageable pageable) {
        Specification<FeedItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("feedType"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (expiryDate != null) {
                predicates.add(cb.equal(root.get("expiryDate"), expiryDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return feedItemRepository.findAll(spec, pageable).map(feedMapper::toResponse);
    }

    // --- Feed Suppliers ---
    @Override
    @Transactional
    public FeedSupplierResponse registerSupplier(FeedSupplierRequest request) {
        log.info("Registering supplier. Code: {}", request.getSupplierCode());

        if (feedSupplierRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new DuplicateRecordException("Supplier code '" + request.getSupplierCode() + "' is already registered.");
        }

        FeedSupplier supplier = feedMapper.toEntity(request);
        FeedSupplier saved = feedSupplierRepository.save(supplier);
        log.info("AUDIT: Supplier Registered. ID: {}, Code: {}", saved.getId(), saved.getSupplierCode());

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedSupplierResponse getSupplierById(Long id) {
        FeedSupplier supplier = feedSupplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + id));
        return feedMapper.toResponse(supplier);
    }

    @Override
    @Transactional
    public FeedSupplierResponse updateSupplier(Long id, FeedSupplierRequest request) {
        log.info("Updating supplier ID: {}", id);

        FeedSupplier supplier = feedSupplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + id));

        if (feedSupplierRepository.existsBySupplierCodeAndIdNot(request.getSupplierCode(), id)) {
            throw new DuplicateRecordException("Supplier code '" + request.getSupplierCode() + "' is already registered.");
        }

        SupplierStatus oldStatus = supplier.getStatus();

        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhoneNumber(request.getPhoneNumber());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setStatus(request.getStatus());
        supplier.setRemarks(request.getRemarks());

        FeedSupplier saved = feedSupplierRepository.save(supplier);

        // Notification Hook: Supplier becomes inactive
        if (oldStatus != SupplierStatus.INACTIVE && saved.getStatus() == SupplierStatus.INACTIVE) {
            notificationRepository.save(Notification.builder()
                    .message("Supplier alert: Supplier '" + saved.getSupplierName() + "' has been set to INACTIVE.")
                    .type("SUPPLIER_INACTIVE")
                    .targetId(saved.getId())
                    .build());
            log.warn("AUDIT: Supplier Inactive Warning. Supplier Code: {}", saved.getSupplierCode());
        }

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        FeedSupplier supplier = feedSupplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + id));
        feedSupplierRepository.delete(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedSupplierResponse> searchSuppliers(SupplierStatus status, Pageable pageable) {
        Specification<FeedSupplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return feedSupplierRepository.findAll(spec, pageable).map(feedMapper::toResponse);
    }

    // --- Feed Purchases ---
    @Override
    @Transactional
    public FeedPurchaseResponse recordFeedPurchase(FeedPurchaseRequest request) {
        log.info("Recording feed purchase. Code: {}", request.getPurchaseCode());

        if (feedPurchaseRepository.existsByPurchaseCode(request.getPurchaseCode())) {
            throw new DuplicateRecordException("Purchase code '" + request.getPurchaseCode() + "' is already registered.");
        }

        FeedSupplier supplier = feedSupplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + request.getSupplierId()));

        FeedItem item = feedItemRepository.findById(request.getFeedItemId())
                .orElseThrow(() -> new NotFoundException("Feed item not found with ID: " + request.getFeedItemId()));

        // Inactive feed cannot be purchased
        if (item.getStatus() == FeedStatus.INACTIVE) {
            throw new ValidationException("Cannot purchase inactive feed items.");
        }

        FeedPurchase purchase = feedMapper.toEntity(request);
        purchase.setSupplier(supplier);
        purchase.setFeedItem(item);

        FeedPurchase saved = feedPurchaseRepository.save(purchase);

        // Increase FeedItem.currentStock
        item.setCurrentStock(item.getCurrentStock() + saved.getQuantity());
        // Propagate stock-based changes
        adjustFeedStatusBasedOnStock(item);
        feedItemRepository.save(item);

        log.info("AUDIT: Feed Purchase Recorded. ID: {}, Code: {}, Quantity: {}", saved.getId(), saved.getPurchaseCode(), saved.getQuantity());

        // Finance Hook integration: Trigger event publishing
        FinanceEvent expense = FinanceEvent.builder()
                .eventType("FEED_PURCHASE_EXPENSE")
                .referenceId(saved.getId())
                .referenceCode(saved.getPurchaseCode())
                .amount(saved.getTotalAmount())
                .description("Feed Expense purchased code " + saved.getPurchaseCode() + " for Item: " + item.getFeedCode())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(expense);

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedPurchaseResponse getPurchaseById(Long id) {
        FeedPurchase purchase = feedPurchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feed purchase ticket not found with ID: " + id));
        return feedMapper.toResponse(purchase);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedPurchaseResponse> searchPurchases(Long supplierId, PaymentStatus paymentStatus, LocalDate purchaseDate, Pageable pageable) {
        Specification<FeedPurchase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (purchaseDate != null) {
                predicates.add(cb.equal(root.get("purchaseDate"), purchaseDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return feedPurchaseRepository.findAll(spec, pageable).map(feedMapper::toResponse);
    }

    // --- Feed Consumption ---
    @Override
    @Transactional
    public FeedConsumptionResponse recordFeedConsumption(FeedConsumptionRequest request) {
        log.info("Recording feed consumption. Date: {}", request.getConsumptionDate());

        FeedItem item = feedItemRepository.findById(request.getFeedItemId())
                .orElseThrow(() -> new NotFoundException("Feed item not found with ID: " + request.getFeedItemId()));

        // Inactive feed cannot be consumed
        if (item.getStatus() == FeedStatus.INACTIVE) {
            throw new ValidationException("Cannot allocate inactive feed items.");
        }

        // Expired feed cannot be allocated
        if (item.getStatus() == FeedStatus.EXPIRED || (item.getExpiryDate() != null && item.getExpiryDate().isBefore(LocalDate.now()))) {
            throw new ValidationException("Cannot allocate expired feed items.");
        }

        // Cannot consume more feed than available
        if (item.getCurrentStock() < request.getQuantity()) {
            throw new ValidationException("Insufficient stock available. Current stock: " + item.getCurrentStock());
        }

        FeedConsumption consumption = feedMapper.toEntity(request);
        consumption.setFeedItem(item);

        // Allocation Rules
        if (request.getChickenId() != null) {
            Chicken chicken = chickenRepository.findById(request.getChickenId())
                    .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + request.getChickenId()));

            // Reject allocation to DEAD or SOLD chickens
            if (chicken.getStatus() == ChickenStatus.DEAD || chicken.getStatus() == ChickenStatus.SOLD) {
                throw new ValidationException("Cannot allocate feed to DEAD or SOLD chickens.");
            }
            consumption.setChicken(chicken);
        }

        if (request.getBrooderBatchId() != null) {
            BrooderBatch batch = brooderBatchRepository.findById(request.getBrooderBatchId())
                    .orElseThrow(() -> new NotFoundException("Brooder batch not found with ID: " + request.getBrooderBatchId()));

            // Reject allocation to Inactive brooder batches
            if (batch.getStatus() != BrooderStatus.ACTIVE) {
                throw new ValidationException("Cannot allocate feed to inactive brooder batches.");
            }
            consumption.setBrooderBatch(batch);
        }

        FeedConsumption saved = feedConsumptionRepository.save(consumption);

        // Decrease stock
        item.setCurrentStock(item.getCurrentStock() - saved.getQuantity());
        adjustFeedStatusBasedOnStock(item);
        feedItemRepository.save(item);

        log.info("AUDIT: Feed Consumption Recorded. ID: {}, Quantity: {}", saved.getId(), saved.getQuantity());

        return feedMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedConsumptionResponse getConsumptionById(Long id) {
        FeedConsumption consumption = feedConsumptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feed consumption record not found with ID: " + id));
        return feedMapper.toResponse(consumption);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedConsumptionResponse> searchConsumptions(Long feedItemId, Long chickenId, Long brooderBatchId, LocalDate consumptionDate, Pageable pageable) {
        Specification<FeedConsumption> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (feedItemId != null) {
                predicates.add(cb.equal(root.get("feedItem").get("id"), feedItemId));
            }
            if (chickenId != null) {
                predicates.add(cb.equal(root.get("chicken").get("id"), chickenId));
            }
            if (brooderBatchId != null) {
                predicates.add(cb.equal(root.get("brooderBatch").get("id"), brooderBatchId));
            }
            if (consumptionDate != null) {
                predicates.add(cb.equal(root.get("consumptionDate"), consumptionDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return feedConsumptionRepository.findAll(spec, pageable).map(feedMapper::toResponse);
    }

    // Prepare service interface
    @Override
    @Transactional
    public void recordGroupFeeding(Long feedItemId, List<Long> chickenIds, Double totalQuantity, LocalDate consumptionDate, String remarks) {
        log.info("Preparing group feeding logic for {} chicken items", chickenIds.size());
        if (chickenIds.isEmpty() || totalQuantity <= 0.0) {
            return;
        }

        Double quantityPerChicken = totalQuantity / chickenIds.size();
        for (Long id : chickenIds) {
            FeedConsumptionRequest req = FeedConsumptionRequest.builder()
                    .feedItemId(feedItemId)
                    .chickenId(id)
                    .consumptionDate(consumptionDate)
                    .quantity(quantityPerChicken)
                    .feedingType(FeedingType.GROUP)
                    .remarks(remarks)
                    .build();
            recordFeedConsumption(req);
        }
    }

    // --- Reporting service helpers ---
    @Override
    public Double getCurrentStock(Long feedItemId) {
        return feedItemRepository.findById(feedItemId)
                .map(FeedItem::getCurrentStock)
                .orElse(0.0);
    }

    @Override
    public List<FeedItemResponse> getLowStockItems() {
        return feedItemRepository.findLowStockItems().stream()
                .map(feedMapper::toResponse)
                .toList();
    }

    @Override
    public Double getMonthlyFeedConsumption(Long feedItemId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return feedConsumptionRepository.findAll().stream()
                .filter(c -> c.getFeedItem().getId().equals(feedItemId))
                .filter(c -> !c.getConsumptionDate().isBefore(start) && !c.getConsumptionDate().isAfter(end))
                .mapToDouble(FeedConsumption::getQuantity)
                .sum();
    }

    @Override
    public Double getFeedCost(Long feedItemId) {
        return feedPurchaseRepository.findAll().stream()
                .filter(p -> p.getFeedItem().getId().equals(feedItemId))
                .mapToDouble(FeedPurchase::getTotalAmount)
                .sum();
    }

    @Override
    public Double getSupplierPurchaseSummary(Long supplierId) {
        return feedPurchaseRepository.findBySupplierId(supplierId).stream()
                .mapToDouble(FeedPurchase::getTotalAmount)
                .sum();
    }

    @Override
    public Double getFeedUsageByChicken(Long chickenId) {
        return feedConsumptionRepository.findByChickenId(chickenId).stream()
                .mapToDouble(FeedConsumption::getQuantity)
                .sum();
    }

    @Override
    public Double getFeedUsageByBrooder(Long brooderBatchId) {
        return feedConsumptionRepository.findByBrooderBatchId(brooderBatchId).stream()
                .mapToDouble(FeedConsumption::getQuantity)
                .sum();
    }

    private void adjustFeedStatusBasedOnStock(FeedItem item) {
        LocalDate today = LocalDate.now();
        if (item.getStatus() == FeedStatus.INACTIVE) {
            // Keep inactive on manual overrides
            return;
        }

        if (item.getExpiryDate() != null && item.getExpiryDate().isBefore(today)) {
            item.setStatus(FeedStatus.EXPIRED);
        } else if (item.getCurrentStock() <= 0.0) {
            item.setStatus(FeedStatus.OUT_OF_STOCK);
        } else {
            item.setStatus(FeedStatus.ACTIVE);
        }

        // Trigger notifications as side-effects:
        // Stock falls below minimum stock
        if (item.getCurrentStock() < item.getMinimumStock() && item.getCurrentStock() > 0.0) {
            notificationRepository.save(Notification.builder()
                    .message("Low Stock Alert: '" + item.getFeedName() + "' stock falls below " + item.getMinimumStock() + " " + item.getUnit())
                    .type("LOW_STOCK_WARNING")
                    .targetId(item.getId())
                    .build());
            log.warn("AUDIT: Low Stock Warning. Item: {}", item.getFeedCode());
        }

        // Out of stock
        if (item.getStatus() == FeedStatus.OUT_OF_STOCK) {
            notificationRepository.save(Notification.builder()
                    .message("Stock Out Alert: '" + item.getFeedName() + "' is completely OUT_OF_STOCK.")
                    .type("OUT_OF_STOCK")
                    .targetId(item.getId())
                    .build());
            log.warn("AUDIT: Out of Stock Warning. Item: {}", item.getFeedCode());
        }

        // Expiry alert
        if (item.getExpiryDate() != null) {
            long days = ChronoUnit.DAYS.between(today, item.getExpiryDate());
            if (days >= 0 && days <= 7) {
                notificationRepository.save(Notification.builder()
                        .message("Expiry Alert: '" + item.getFeedName() + "' expires soon on " + item.getExpiryDate())
                        .type("EXPIRY_WARNING")
                        .targetId(item.getId())
                        .build());
                log.warn("AUDIT: Feed Expiring soon. Item: {}", item.getFeedCode());
            }
            if (days < 0) {
                log.warn("AUDIT: Feed Expired. Item: {}", item.getFeedCode());
            }
        }
    }
}
