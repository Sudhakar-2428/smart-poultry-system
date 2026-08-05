package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "egg_collections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Female chicken (Hen) is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "female_chicken_id", nullable = false)
    private Chicken femaleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_chicken_id")
    private Chicken maleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeding_pair_id")
    private BreedingPair breedingPair;

    @Column(name = "pairing_date")
    private LocalDate pairingDate;

    @Column(name = "egg_laying_started_date", nullable = false)
    private LocalDate eggLayingStartedDate;

    @Builder.Default
    @Column(name = "current_batch_number", nullable = false)
    private Integer currentBatchNumber = 1;

    @Builder.Default
    @Column(name = "today_egg_count", nullable = false)
    private Integer todayEggCount = 0;

    @Builder.Default
    @Column(name = "weekly_egg_count", nullable = false)
    private Integer weeklyEggCount = 0;

    @Builder.Default
    @Column(name = "monthly_egg_count", nullable = false)
    private Integer monthlyEggCount = 0;

    @Builder.Default
    @Column(name = "total_egg_count", nullable = false)
    private Integer totalEggCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggCollectionStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
