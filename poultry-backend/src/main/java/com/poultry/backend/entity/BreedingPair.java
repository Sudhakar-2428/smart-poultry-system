package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "breeding_pairs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreedingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pair code is required")
    @Column(name = "pair_code", nullable = false, unique = true, length = 50)
    private String pairCode;

    @NotNull(message = "Male chicken is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_chicken_id", nullable = false)
    private Chicken maleChicken;

    @NotNull(message = "Female chicken is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "female_chicken_id", nullable = false)
    private Chicken femaleChicken;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull(message = "Pair status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PairStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "breeding_purpose", nullable = false, length = 50)
    private BreedingPurpose breedingPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "pairing_type", length = 30)
    @Builder.Default
    private PairingType pairingType = PairingType.NATURAL;

    @Column(name = "expected_egg_laying_date")
    private LocalDate expectedEggLayingDate;

    @Column(name = "egg_laying_started_at")
    private LocalDateTime eggLayingStartedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "expected_egg_production")
    private Integer expectedEggProduction;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
