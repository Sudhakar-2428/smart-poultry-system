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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_hen_id")
    private Chicken sourceHen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_chicken_id")
    private Chicken maleChicken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeding_pair_id")
    private BreedingPair breedingPair;

    @Enumerated(EnumType.STRING)
    @Column(name = "incubation_method", length = 50)
    private IncubationMethod incubationMethod;

    @Column(name = "incubator_number", length = 50)
    private String incubatorNumber;

    @Column(name = "tray_number", length = 50)
    private String trayNumber;

    @Column(name = "turning_schedule", length = 100)
    private String turningSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broody_hen_id")
    private Chicken broodyHen;

    @Column(name = "nest_location", length = 100)
    private String nestLocation;

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
