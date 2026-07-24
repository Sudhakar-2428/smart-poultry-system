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
@Table(name = "incubator_batches", uniqueConstraints = {
        @UniqueConstraint(columnNames = "batch_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncubatorBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Batch code is required")
    @Column(name = "batch_code", nullable = false, length = 50)
    private String batchCode;

    @NotNull(message = "Egg batch is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "egg_batch_id", nullable = false)
    private EggBatch eggBatch;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Expected hatch date is required")
    @Column(name = "expected_hatch_date", nullable = false)
    private LocalDate expectedHatchDate;

    @Column(name = "actual_hatch_date")
    private LocalDate actualHatchDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncubatorStatus status;

    private Double temperature;

    private Double humidity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
