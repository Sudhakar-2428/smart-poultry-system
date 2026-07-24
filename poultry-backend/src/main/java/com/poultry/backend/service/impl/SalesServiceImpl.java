package com.poultry.backend.service.impl;

import com.poultry.backend.common.FinanceEvent;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.SalesMapper;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.SalesService;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ChickenRepository chickenRepository;
    private final EggBatchRepository eggBatchRepository;
    private final FarmSettingRepository farmSettingRepository;
    private final NotificationRepository notificationRepository;
    private final SalesMapper salesMapper;
    private final ApplicationEventPublisher eventPublisher;

    // --- Customer CRM Operations ---
    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("CRM: Creating customer with code: {}", request.getCustomerCode());

        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new DuplicateRecordException("Customer code '" + request.getCustomerCode() + "' is already registered.");
        }

        Customer customer = salesMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        log.info("AUDIT: Customer Created. ID: {}, Code: {}", saved.getId(), saved.getCustomerCode());

        return salesMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));
        return salesMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("CRM: Updating customer ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));

        if (customerRepository.existsByCustomerCodeAndIdNot(request.getCustomerCode(), id)) {
            throw new DuplicateRecordException("Customer code '" + request.getCustomerCode() + "' is already registered.");
        }

        customer.setCustomerCode(request.getCustomerCode());
        customer.setCustomerName(request.getCustomerName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setCustomerType(request.getCustomerType());
        customer.setStatus(request.getStatus());
        customer.setRemarks(request.getRemarks());

        Customer saved = customerRepository.save(customer);
        return salesMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("CRM: Deleting customer ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));
        customerRepository.delete(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(CustomerStatus status, Pageable pageable) {
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return customerRepository.findAll(spec, pageable).map(salesMapper::toResponse);
    }

    // --- Sales Orders Processing ---
    @Override
    @Transactional
    public SalesOrderResponse createSalesOrder(SalesOrderRequest request) {
        log.info("Order Registry: Creating sales order. Number: {}", request.getOrderNumber());

        if (salesOrderRepository.existsByOrderNumber(request.getOrderNumber())) {
            throw new DuplicateRecordException("Order number '" + request.getOrderNumber() + "' is already registered.");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + request.getCustomerId()));

        SalesOrder order = salesMapper.toEntity(request);
        order.setCustomer(customer);

        // Map items and calculate subtotal
        double calculatedSubtotal = 0.0;
        for (SalesOrderItemRequest itemReq : request.getItems()) {
            SalesOrderItem item = salesMapper.toEntity(itemReq);
            item.setSalesOrder(order);

            // Fetch relations
            if (itemReq.getItemType() == ItemType.CHICKEN) {
                if (itemReq.getChickenId() == null) {
                    throw new ValidationException("Chicken ID is required for CHICKEN items.");
                }
                Chicken chicken = chickenRepository.findById(itemReq.getChickenId())
                        .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + itemReq.getChickenId()));
                item.setChicken(chicken);
            } else if (itemReq.getItemType() == ItemType.EGG_BATCH) {
                if (itemReq.getEggBatchId() == null) {
                    throw new ValidationException("Egg batch ID is required for EGG_BATCH items.");
                }
                EggBatch eggBatch = eggBatchRepository.findById(itemReq.getEggBatchId())
                        .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + itemReq.getEggBatchId()));
                item.setEggBatch(eggBatch);
            }

            calculatedSubtotal += item.getTotalPrice();
            order.getItems().add(item);
        }

        order.setSubtotal(calculatedSubtotal);
        double total = calculatedSubtotal + order.getTax() - order.getDiscount();
        order.setTotalAmount(Math.max(0.0, total));
        order.setBalanceAmount(Math.max(0.0, order.getTotalAmount() - order.getAmountPaid()));

        // Apply inventory changes if confirmed or completed
        if (order.getStatus() == SalesOrderStatus.CONFIRMED || order.getStatus() == SalesOrderStatus.COMPLETED) {
            applyInventoryChanges(order);
        }

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("AUDIT: Sales Order Created. ID: {}, Number: {}", saved.getId(), saved.getOrderNumber());

        // Process status alerts and notification integrations
        handlePostOrderPersistActions(saved, null);

        return salesMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse getSalesOrderById(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found with ID: " + id));
        return salesMapper.toResponse(order);
    }

    @Override
    @Transactional
    public SalesOrderResponse updateSalesOrder(Long id, SalesOrderRequest request) {
        log.info("Order Registry: Updating sales order ID: {}", id);

        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found with ID: " + id));

        // Rule: Completed orders cannot be edited.
        if (order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new ValidationException("Completed orders cannot be edited.");
        }

        if (salesOrderRepository.existsByOrderNumberAndIdNot(request.getOrderNumber(), id)) {
            throw new DuplicateRecordException("Order number '" + request.getOrderNumber() + "' is already registered.");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + request.getCustomerId()));

        SalesOrderStatus oldStatus = order.getStatus();

        // 1. Rollback old inventory if it was active
        if (oldStatus == SalesOrderStatus.CONFIRMED || oldStatus == SalesOrderStatus.COMPLETED) {
            rollbackInventoryChanges(order);
        }

        // Wipe old items list
        order.getItems().clear();

        // 2. Map and build new details
        order.setCustomer(customer);
        order.setOrderNumber(request.getOrderNumber());
        order.setOrderDate(request.getOrderDate());
        order.setSaleType(request.getSaleType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentStatus());
        order.setDiscount(request.getDiscount());
        order.setTax(request.getTax());
        order.setAmountPaid(request.getAmountPaid());
        order.setStatus(request.getStatus());
        order.setRemarks(request.getRemarks());

        double calculatedSubtotal = 0.0;
        for (SalesOrderItemRequest itemReq : request.getItems()) {
            SalesOrderItem item = salesMapper.toEntity(itemReq);
            item.setSalesOrder(order);

            if (itemReq.getItemType() == ItemType.CHICKEN) {
                Chicken chicken = chickenRepository.findById(itemReq.getChickenId())
                        .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + itemReq.getChickenId()));
                item.setChicken(chicken);
            } else if (itemReq.getItemType() == ItemType.EGG_BATCH) {
                EggBatch eggBatch = eggBatchRepository.findById(itemReq.getEggBatchId())
                        .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + itemReq.getEggBatchId()));
                item.setEggBatch(eggBatch);
            }

            calculatedSubtotal += item.getTotalPrice();
            order.getItems().add(item);
        }

        order.setSubtotal(calculatedSubtotal);
        double total = calculatedSubtotal + order.getTax() - order.getDiscount();
        order.setTotalAmount(Math.max(0.0, total));
        order.setBalanceAmount(Math.max(0.0, order.getTotalAmount() - order.getAmountPaid()));

        // 3. Apply new inventory configurations if status is confirmed or completed
        if (order.getStatus() == SalesOrderStatus.CONFIRMED || order.getStatus() == SalesOrderStatus.COMPLETED) {
            applyInventoryChanges(order);
        }

        SalesOrder saved = salesOrderRepository.save(order);

        // Process status alerts and notifications
        handlePostOrderPersistActions(saved, oldStatus);

        return salesMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SalesOrderResponse updateOrderStatus(Long id, SalesOrderStatusRequest request) {
        log.info("Order Registry: Patching status for sales order ID: {} to {}", id, request.getStatus());

        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found with ID: " + id));

        // Rule: Completed orders cannot be edited.
        if (order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new ValidationException("Completed orders cannot be edited.");
        }

        SalesOrderStatus oldStatus = order.getStatus();

        // Rollback if transitioning from a state that decreased stock
        if (oldStatus == SalesOrderStatus.CONFIRMED || oldStatus == SalesOrderStatus.COMPLETED) {
            rollbackInventoryChanges(order);
        }

        order.setStatus(request.getStatus());
        if (request.getRemarks() != null) {
            order.setRemarks(request.getRemarks());
        }

        // Reapply inventory if confirmed or completed
        if (order.getStatus() == SalesOrderStatus.CONFIRMED || order.getStatus() == SalesOrderStatus.COMPLETED) {
            applyInventoryChanges(order);
        }

        SalesOrder saved = salesOrderRepository.save(order);

        handlePostOrderPersistActions(saved, oldStatus);

        return salesMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryResponse> searchSalesOrders(
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
    ) {
        log.info("Searching sales orders with filters");

        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (saleType != null) {
                predicates.add(cb.equal(root.get("saleType"), saleType));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (orderStatus != null) {
                predicates.add(cb.equal(root.get("status"), orderStatus));
            }
            if (orderDate != null) {
                predicates.add(cb.equal(root.get("orderDate"), orderDate));
            }
            if (chickenId != null || eggBatchId != null) {
                // Must join with items
                jakarta.persistence.criteria.Join<SalesOrder, SalesOrderItem> joinedItems = root.join("items");
                if (chickenId != null) {
                    predicates.add(cb.equal(joinedItems.get("chicken").get("id"), chickenId));
                }
                if (eggBatchId != null) {
                    predicates.add(cb.equal(joinedItems.get("eggBatch").get("id"), eggBatchId));
                }
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return salesOrderRepository.findAll(spec, pageable).map(salesMapper::toSummaryResponse);
    }

    // --- Reports Integration hooks ---
    @Override
    public Double getTotalRevenue() {
        return salesOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();
    }

    @Override
    public Double getSalesByCustomer(Long customerId) {
        return salesOrderRepository.findByCustomerId(customerId).stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();
    }

    @Override
    public Double getSalesByChicken(Long chickenId) {
        return salesOrderItemRepository.findByChickenId(chickenId).stream()
                .filter(item -> item.getSalesOrder().getStatus() == SalesOrderStatus.COMPLETED)
                .mapToDouble(SalesOrderItem::getTotalPrice)
                .sum();
    }

    @Override
    public Double getSalesByEggBatch(Long eggBatchId) {
        return salesOrderItemRepository.findByEggBatchId(eggBatchId).stream()
                .filter(item -> item.getSalesOrder().getStatus() == SalesOrderStatus.COMPLETED)
                .mapToDouble(SalesOrderItem::getTotalPrice)
                .sum();
    }

    @Override
    public Double getDailySales(LocalDate date) {
        return salesOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .filter(o -> o.getOrderDate().equals(date))
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();
    }

    @Override
    public Double getMonthlySales(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return salesOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == SalesOrderStatus.COMPLETED)
                .filter(o -> !o.getOrderDate().isBefore(start) && !o.getOrderDate().isAfter(end))
                .mapToDouble(SalesOrder::getTotalAmount)
                .sum();
    }

    // --- Internal Helpers ---
    private void applyInventoryChanges(SalesOrder order) {
        for (SalesOrderItem item : order.getItems()) {
            if (item.getItemType() == ItemType.CHICKEN) {
                Chicken chicken = item.getChicken();
                if (chicken != null) {
                    // Check rules
                    if (chicken.getStatus() == ChickenStatus.SOLD) {
                        throw new ValidationException("Chicken " + chicken.getChickenCode() + " has already been sold.");
                    }
                    if (chicken.getStatus() == ChickenStatus.DEAD) {
                        throw new ValidationException("Cannot sell deceased chicken " + chicken.getChickenCode());
                    }
                    if (chicken.getStatus() != ChickenStatus.ACTIVE) {
                        throw new ValidationException("Only ACTIVE chickens can be sold. Chicken " + chicken.getChickenCode() + " is " + chicken.getStatus());
                    }

                    chicken.setStatus(ChickenStatus.SOLD);
                    chicken.setSaleDate(order.getOrderDate());
                    chickenRepository.save(chicken);
                    log.info("AUDIT: Chicken Sold. Code: {}", chicken.getChickenCode());
                }
            } else if (item.getItemType() == ItemType.EGG_BATCH) {
                EggBatch eggBatch = item.getEggBatch();
                if (eggBatch != null) {
                    if (eggBatch.getGoodEggs() < item.getQuantity().intValue()) {
                        throw new ValidationException("Egg batch " + eggBatch.getBatchCode() + " does not have sufficient eggs. Available: " + eggBatch.getGoodEggs() + ", Required: " + item.getQuantity().intValue());
                    }
                    eggBatch.setGoodEggs(eggBatch.getGoodEggs() - item.getQuantity().intValue());
                    if (eggBatch.getGoodEggs() <= 0) {
                        eggBatch.setStatus(EggBatchStatus.SOLD);
                    }
                    eggBatchRepository.save(eggBatch);
                    log.info("AUDIT: Egg Batch Sold. Code: {}, Eggs sold: {}", eggBatch.getBatchCode(), item.getQuantity());
                }
            }
        }
    }

    private void rollbackInventoryChanges(SalesOrder order) {
        for (SalesOrderItem item : order.getItems()) {
            if (item.getItemType() == ItemType.CHICKEN) {
                Chicken chicken = item.getChicken();
                if (chicken != null) {
                    chicken.setStatus(ChickenStatus.ACTIVE);
                    chicken.setSaleDate(null);
                    chickenRepository.save(chicken);
                }
            } else if (item.getItemType() == ItemType.EGG_BATCH) {
                EggBatch eggBatch = item.getEggBatch();
                if (eggBatch != null) {
                    boolean wasSold = eggBatch.getStatus() == EggBatchStatus.SOLD;
                    eggBatch.setGoodEggs(eggBatch.getGoodEggs() + item.getQuantity().intValue());
                    if (wasSold) {
                        eggBatch.setStatus(EggBatchStatus.CREATED); // revert to created if it was fully sold out before
                    }
                    eggBatchRepository.save(eggBatch);
                }
            }
        }
    }

    private void handlePostOrderPersistActions(SalesOrder saved, SalesOrderStatus oldStatus) {
        LocalDate today = LocalDate.now();

        // 1. Finance Integration: Publish FinanceEvent for COMPLETED sale
        if (saved.getStatus() == SalesOrderStatus.COMPLETED && oldStatus != SalesOrderStatus.COMPLETED) {
            FinanceEvent expense = FinanceEvent.builder()
                    .eventType("SALES_REVENUE")
                    .referenceId(saved.getId())
                    .referenceCode(saved.getOrderNumber())
                    .amount(saved.getTotalAmount())
                    .description("Sales Revenue recorded for Code: " + saved.getOrderNumber() + ", Customer: " + saved.getCustomer().getCustomerName())
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(expense);

            log.info("AUDIT: Payment Received. Order Number: {}, Amount: {}", saved.getOrderNumber(), saved.getTotalAmount());
        }

        // 2. Large sale notification
        Double largeSaleThreshold = 5000.0;
        java.util.Optional<FarmSetting> settingOpt = farmSettingRepository.findById("large_sale_threshold");
        if (settingOpt.isPresent()) {
            try {
                largeSaleThreshold = Double.parseDouble(settingOpt.get().getValue());
            } catch (NumberFormatException ignored) {}
        }
        if (saved.getTotalAmount() >= largeSaleThreshold) {
            notificationRepository.save(Notification.builder()
                    .message("Large Sale Event: Sales order '" + saved.getOrderNumber() + "' totals " + saved.getTotalAmount() + ", exceeding large sale limit.")
                    .type("LARGE_SALE_ALERT")
                    .targetId(saved.getId())
                    .build());
        }

        // 3. Outstanding payment overdue notification
        if (saved.getPaymentStatus() == PaymentStatus.PENDING || saved.getPaymentStatus() == PaymentStatus.PARTIAL) {
            if (saved.getOrderDate().isBefore(today)) {
                notificationRepository.save(Notification.builder()
                        .message("Outstanding payment warning: Sales order '" + saved.getOrderNumber() + "' remains unpaid paste due date.")
                        .type("OUTSTANDING_PAYMENT_OVERDUE")
                        .targetId(saved.getId())
                        .build());
            }
        }

        // 4. Order Cancelled notification and audits
        if (saved.getStatus() == SalesOrderStatus.CANCELLED && oldStatus != SalesOrderStatus.CANCELLED) {
            notificationRepository.save(Notification.builder()
                    .message("Cancellation Alert: Sales order '" + saved.getOrderNumber() + "' has been cancelled.")
                    .type("ORDER_CANCELLED")
                    .targetId(saved.getId())
                    .build());
            log.warn("AUDIT: Order Cancelled. Order Number: {}", saved.getOrderNumber());
        }
    }
}
