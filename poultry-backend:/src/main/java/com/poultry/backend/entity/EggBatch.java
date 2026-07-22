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

    @NotNull(message = "Total eggs is required")
    @Min(value = 1, message = "Total eggs must be greater than zero")
    @Column(name = "total_eggs", nullable = false)
    private Integer totalEggs;

    @NotNull(message = "Good eggs is required")
    @Min(value = 0, message = "Good eggs count cannot be negative")
    @Column(name = "good_eggs", nullable = false)
    private Integer goodEggs;

    @NotNull(message = "Damaged eggs is required")
    @Min(value = 0, message = "Damaged eggs count cannot be negative")
    @Column(name = "damaged_eggs", nullable = false)
    private Integer damagedEggs;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggBatchStatus status;

    @NotNull(message = "Purpose is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EggPurpose purpose;

    @NotNull(message = "Expected hatch date is required")
    @Column(name = "expected_hatch_date", nullable = false)
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
