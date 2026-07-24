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
@Table(name = "chick_growth_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickGrowthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Chicken is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chicken_id", nullable = false)
    private Chicken chicken;

    @NotNull(message = "Growth date is required")
    @Column(name = "growth_date", nullable = false)
    private LocalDate growthDate;

    @NotNull(message = "Age in days is required")
    @Column(name = "age_in_days", nullable = false)
    private Integer ageInDays;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be greater than zero")
    private Double weight;

    private Double height;

    @NotNull(message = "Health status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    private HealthStatus healthStatus;

    @NotNull(message = "Growth stage is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "growth_stage", nullable = false, length = 50)
    private GrowthStage growthStage;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
