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
@Table(name = "egg_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Record date is required")
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @NotNull(message = "Hen is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hen_id", nullable = false)
    private Chicken hen;

    @NotNull(message = "Number of eggs is required")
    @Min(value = 1, message = "Egg count must be greater than zero")
    @Column(name = "number_of_eggs", nullable = false)
    private Integer numberOfEggs;

    @NotNull(message = "Damaged eggs is required")
    @Min(value = 0, message = "Damaged eggs count cannot be negative")
    @Column(name = "damaged_eggs", nullable = false)
    private Integer damagedEggs;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
