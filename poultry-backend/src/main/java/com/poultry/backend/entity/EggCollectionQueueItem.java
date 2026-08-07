package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "egg_collection_queue_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggCollectionQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Queue date is required")
    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @NotNull(message = "Chicken ID is required")
    @Column(name = "chicken_id", nullable = false)
    private Long chickenId;

    @Column(name = "hen_code", length = 50)
    private String henCode;

    @Column(name = "hen_name", length = 100)
    private String henName;

    @Column(length = 100)
    private String breed;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "pairing_code", length = 50)
    private String pairingCode;

    @Column(name = "egg_laying_start_date")
    private LocalDate eggLayingStartDate;

    @Builder.Default
    @Column(name = "current_egg_count", nullable = false)
    private Integer currentEggCount = 0;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, COMPLETED, RESCHEDULED, ESCALATED

    @Column(name = "no_egg_reason", length = 255)
    private String noEggReason;

    @Builder.Default
    @Column(name = "healthy_eggs")
    private Integer healthyEggs = 0;

    @Builder.Default
    @Column(name = "broken_eggs")
    private Integer brokenEggs = 0;

    @Builder.Default
    @Column(name = "damaged_eggs")
    private Integer damagedEggs = 0;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "assigned_worker_email", length = 100)
    private String assignedWorkerEmail;

    @Column(name = "rescheduled_until")
    private LocalDateTime rescheduledUntil;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
