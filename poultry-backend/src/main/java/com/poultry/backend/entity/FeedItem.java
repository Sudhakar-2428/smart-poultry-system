package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Feed code is required")
    @Column(name = "feed_code", nullable = false, unique = true, length = 50)
    private String feedCode;

    @NotNull(message = "Feed name is required")
    @Column(name = "feed_name", nullable = false, length = 100)
    private String feedName;

    @NotNull(message = "Feed type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false, length = 50)
    private FeedType feedType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Unit is required")
    @Column(nullable = false, length = 20)
    private String unit;

    @Builder.Default
    @Column(name = "minimum_stock", nullable = false)
    private Double minimumStock = 0.0;

    @Builder.Default
    @Column(name = "current_stock", nullable = false)
    private Double currentStock = 0.0;

    @Builder.Default
    @Column(name = "unit_cost", nullable = false)
    private Double unitCost = 0.0;

    @NotNull(message = "Storage location is required")
    @Column(name = "storage_location", nullable = false, length = 100)
    private String storageLocation;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @NotNull(message = "Feed status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeedStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
