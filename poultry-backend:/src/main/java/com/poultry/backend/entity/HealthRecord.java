package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Record code is required")
    @Column(name = "record_code", nullable = false, unique = true, length = 50)
    private String recordCode;

    @NotNull(message = "Chicken reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chicken_id", nullable = false)
    private Chicken chicken;

    @NotNull(message = "Record date is required")
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @NotNull(message = "Health type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "health_type", nullable = false, length = 50)
    private HealthType healthType;

    @Column(name = "disease_name", length = 100)
    private String diseaseName;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column(name = "medicine_name", length = 100)
    private String medicineName;

    @Column(name = "medicine_dose", length = 100)
    private String medicineDose;

    @Column(name = "vaccination_name", length = 100)
    private String vaccinationName;

    @Column(name = "vaccination_batch", length = 50)
    private String vaccinationBatch;

    @Column(name = "next_vaccination_date")
    private LocalDate nextVaccinationDate;

    @NotNull(message = "Veterinarian name is required")
    @Column(length = 100, nullable = false)
    private String veterinarian;

    @NotNull(message = "Health status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    private HealthStatus healthStatus;

    @Builder.Default
    @Column(nullable = false)
    private Boolean mortality = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
