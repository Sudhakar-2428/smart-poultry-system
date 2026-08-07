package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "egg_batches", uniqueConstraints = {
        @UniqueConstraint(columnNames = "batch_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Batch code is required")
    @Column(name = "batch_code", nullable = false, length = 50)
    private String batchCode;

    @NotNull(message = "Batch date is required")
    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_hen_id")
    private Chicken sourceHen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_chicken_id")
    private Chicken maleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeding_pair_id")
    private BreedingPair breedingPair;

    @Column(name = "batch_number")
    private Integer batchNumber;

    @Builder.Default
    @Column(name = "total_eggs", nullable = false)
    private Integer totalEggs = 0;

    @Builder.Default
    @Column(name = "good_eggs", nullable = false)
    private Integer goodEggs = 0;

    @Builder.Default
    @Column(name = "damaged_eggs", nullable = false)
    private Integer damagedEggs = 0;

    @Builder.Default
    @Column(name = "broken_eggs", nullable = false)
    private Integer brokenEggs = 0;

    @Builder.Default
    @Column(name = "cracked_eggs", nullable = false)
    private Integer crackedEggs = 0;

    @Builder.Default
    @Column(name = "double_yolk_eggs", nullable = false)
    private Integer doubleYolkEggs = 0;

    @Builder.Default
    @Column(name = "selected_for_hatching", nullable = false)
    private Integer selectedForHatching = 0;

    @Builder.Default
    @Column(name = "selected_for_sale", nullable = false)
    private Integer selectedForSale = 0;

    @Builder.Default
    @Column(name = "selected_for_home_use", nullable = false)
    private Integer selectedForHomeUse = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggBatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EggPurpose purpose;

    @Column(name = "expected_hatch_date")
    private LocalDate expectedHatchDate;

    @Column(name = "actual_hatch_date")
    private LocalDate actualHatchDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
