package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "egg_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = "egg_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Egg code is required")
    @Column(name = "egg_code", nullable = false, length = 50)
    private String eggCode;

    @NotNull(message = "Female chicken is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "female_chicken_id", nullable = false)
    private Chicken femaleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_chicken_id")
    private Chicken maleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeding_pair_id")
    private BreedingPair breedingPair;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "egg_collection_id")
    private EggCollection eggCollection;

    @NotNull(message = "Batch number is required")
    @Column(name = "batch_number", nullable = false)
    private Integer batchNumber;

    @NotNull(message = "Collection date is required")
    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggItemStatus status;

    @NotNull(message = "Purpose is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggPurpose purpose;

    @Builder.Default
    @Column(name = "is_moved_to_hatching", nullable = false)
    private Boolean isMovedToHatching = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
