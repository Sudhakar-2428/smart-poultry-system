package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hatching_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = "report_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HatchingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Report code is required")
    @Column(name = "report_code", nullable = false, length = 50)
    private String reportCode;

    @NotNull(message = "Incubator batch is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incubator_batch_id", nullable = false)
    private IncubatorBatch incubatorBatch;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "farm_name", length = 100)
    private String farmName;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    // Mother Hen Details
    @Column(name = "mother_hen_code", length = 50)
    private String motherHenCode;

    @Column(name = "mother_hen_name", length = 100)
    private String motherHenName;

    @Column(name = "mother_hen_breed", length = 50)
    private String motherHenBreed;

    @Column(name = "mother_hen_age", length = 50)
    private String motherHenAge;

    @Column(name = "mother_hen_origin", length = 50)
    private String motherHenOrigin;

    // Father Rooster Details
    @Column(name = "father_rooster_code", length = 50)
    private String fatherRoosterCode;

    @Column(name = "father_rooster_name", length = 100)
    private String fatherRoosterName;

    @Column(name = "father_rooster_breed", length = 50)
    private String fatherRoosterBreed;

    @Column(name = "father_rooster_age", length = 50)
    private String fatherRoosterAge;

    @Column(name = "father_rooster_origin", length = 50)
    private String fatherRoosterOrigin;

    // Breeding Details
    @Column(name = "pairing_code", length = 50)
    private String pairingCode;

    @Column(name = "pairing_date")
    private LocalDate pairingDate;

    @Column(name = "egg_laying_start_date")
    private LocalDate eggLayingStartDate;

    @Column(name = "collection_period_days")
    private Integer collectionPeriodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "incubation_method", length = 50)
    private IncubationMethod incubationMethod;

    @Column(name = "equipment_or_nest", length = 100)
    private String equipmentOrNest;

    // Egg Summary
    private Integer totalEggsCollected;
    private Integer eggsSelectedForHatching;
    private Integer healthyEggs;
    private Integer brokenEggs;
    private Integer rejectedEggs;

    // Candling Summary
    private Integer day7Fertile;
    private Integer day7Infertile;
    private Integer day7DeadEmbryos;

    private Integer day14Fertile;
    private Integer day14Infertile;
    private Integer day14DeadEmbryos;

    private Integer day18Fertile;
    private Integer day18Infertile;
    private Integer day18DeadEmbryos;

    // Hatch Results
    private Integer totalEggsSet;
    private Integer fertileEggs;
    private Integer hatchedChicks;
    private Integer healthyChicks;
    private Integer weakChicks;
    private Integer deadChicks;
    private Integer unhatchedEggs;
    private Double hatchSuccessPercentage;

    // Performance Analysis
    private Double fertilityRate;
    private Double hatchSuccessRate;
    private Double healthyChickRate;
    private Double lossPercentage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
