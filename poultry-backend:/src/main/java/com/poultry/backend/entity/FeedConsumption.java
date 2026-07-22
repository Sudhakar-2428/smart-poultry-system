package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_consumptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Feed item reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_item_id", nullable = false)
    private FeedItem feedItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chicken_id")
    private Chicken chicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brooder_batch_id")
    private BrooderBatch brooderBatch;

    @NotNull(message = "Consumption date is required")
    @Column(name = "consumption_date", nullable = false)
    private LocalDate consumptionDate;

    @NotNull(message = "Quantity is required")
    @Column(nullable = false)
    private Double quantity;

    @NotNull(message = "Feeding type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "feeding_type", nullable = false, length = 50)
    private FeedingType feedingType;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
