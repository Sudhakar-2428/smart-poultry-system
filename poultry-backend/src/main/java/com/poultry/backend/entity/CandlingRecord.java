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
@Table(name = "candling_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandlingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Incubator batch is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incubator_batch_id", nullable = false)
    private IncubatorBatch incubatorBatch;

    @NotNull(message = "Candling date is required")
    @Column(name = "candling_date", nullable = false)
    private LocalDate candlingDate;

    @NotNull(message = "Candling day is required")
    @Column(name = "candling_day", nullable = false)
    private Integer candlingDay;

    @NotNull(message = "Fertile eggs count is required")
    @Min(value = 0, message = "Fertile eggs count cannot be negative")
    @Column(name = "fertile_eggs", nullable = false)
    private Integer fertileEggs;

    @NotNull(message = "Infertile eggs count is required")
    @Min(value = 0, message = "Infertile eggs count cannot be negative")
    @Column(name = "infertile_eggs", nullable = false)
    private Integer infertileEggs;

    @NotNull(message = "Dead embryos count is required")
    @Min(value = 0, message = "Dead embryos count cannot be negative")
    @Column(name = "dead_embryos", nullable = false)
    private Integer deadEmbryos;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
