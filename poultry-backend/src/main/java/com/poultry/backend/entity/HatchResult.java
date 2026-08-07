package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hatch_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Incubator batch is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incubator_batch_id", nullable = false)
    private IncubatorBatch incubatorBatch;

    @NotNull(message = "Total eggs is required")
    @Min(value = 0, message = "Total eggs cannot be negative")
    private Integer totalEggs;

    @NotNull(message = "Fertile eggs is required")
    @Min(value = 0, message = "Fertile eggs cannot be negative")
    private Integer fertileEggs;

    @NotNull(message = "Hatched chicks is required")
    @Min(value = 0, message = "Hatched chicks cannot be negative")
    private Integer hatchedChicks;

    @Min(value = 0, message = "Healthy chicks cannot be negative")
    private Integer healthyChicks;

    @Min(value = 0, message = "Weak chicks cannot be negative")
    private Integer weakChicks;

    @Min(value = 0, message = "Dead chicks cannot be negative")
    private Integer deadChicks;

    @NotNull(message = "Dead embryos is required")
    @Min(value = 0, message = "Dead embryos cannot be negative")
    private Integer deadEmbryos;

    @NotNull(message = "Unhatched eggs is required")
    @Min(value = 0, message = "Unhatched eggs cannot be negative")
    private Integer unhatchedEggs;

    @NotNull(message = "Hatch percentage is required")
    @Column(name = "hatch_percentage", nullable = false)
    private Double hatchPercentage;

    @NotNull(message = "Recorded date is required")
    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
