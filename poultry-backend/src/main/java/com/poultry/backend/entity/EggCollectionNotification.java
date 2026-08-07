package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "egg_collection_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggCollectionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Chicken ID is required")
    @Column(name = "chicken_id", nullable = false)
    private Long chickenId;

    @Column(name = "hen_code", length = 50)
    private String henCode;

    @Column(name = "hen_name", length = 100)
    private String henName;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @NotNull(message = "Notification date is required")
    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, NO_EGG, ESCALATED, OVERDUE, DISMISSED

    @Column(name = "no_egg_reason", length = 100)
    private String noEggReason;

    @Column(name = "rescheduled_until")
    private LocalDateTime rescheduledUntil;

    @Column(name = "healthy_eggs")
    private Integer healthyEggs;

    @Column(name = "broken_eggs")
    private Integer brokenEggs;

    @Column(name = "damaged_eggs")
    private Integer damagedEggs;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "escalated_to_worker")
    @Builder.Default
    private Boolean escalatedToWorker = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "manager_emailed_at")
    private LocalDateTime managerEmailedAt;

    @Column(name = "assigned_worker_email", length = 100)
    private String assignedWorkerEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
