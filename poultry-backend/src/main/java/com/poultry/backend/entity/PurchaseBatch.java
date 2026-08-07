package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_batches", uniqueConstraints = {@UniqueConstraint(columnNames = "batch_code")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Batch code is required")
    @Column(name = "batch_code", nullable = false, length = 50)
    private String batchCode;

    @NotNull(message = "Supplier name is required")
    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(name = "supplier_contact", length = 50)
    private String supplierContact;

    @NotNull(message = "Purchase date is required")
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "transport_cost", precision = 12, scale = 2)
    private BigDecimal transportCost;

    @Column(name = "total_chickens_count")
    @Builder.Default
    private Integer totalChickensCount = 0;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
