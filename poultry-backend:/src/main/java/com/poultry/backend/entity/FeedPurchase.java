package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_purchases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Purchase code is required")
    @Column(name = "purchase_code", nullable = false, unique = true, length = 50)
    private String purchaseCode;

    @NotNull(message = "Supplier reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private FeedSupplier supplier;

    @NotNull(message = "Purchase date is required")
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @NotNull(message = "Feed item reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_item_id", nullable = false)
    private FeedItem feedItem;

    @NotNull(message = "Quantity is required")
    @Column(nullable = false)
    private Double quantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @NotNull(message = "Total amount is required")
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
