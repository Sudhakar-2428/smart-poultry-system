package com.poultry.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chickens", uniqueConstraints = {
        @UniqueConstraint(columnNames = "chicken_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chicken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Chicken code is required")
    @Column(name = "chicken_code", nullable = false, length = 50)
    private String chickenCode;

    @Column(length = 100)
    private String name;

    @NotNull(message = "Breed is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Breed breed;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChickenCategory category;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    private Double weight;

    @Column(length = 50)
    private String color;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChickenStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 50)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ChickenOrigin origin;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost")
    private Double purchaseCost;

    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "supplier_contact", length = 50)
    private String supplierContact;

    @Column(name = "wing_tag_number", length = 50)
    private String wingTagNumber;

    @Column(name = "leg_band_number", length = 50)
    private String legBandNumber;

    private Boolean vaccinated;

    @ElementCollection
    @CollectionTable(name = "chicken_vaccinations", joinColumns = @JoinColumn(name = "chicken_id"))
    @Builder.Default
    private List<ChickenVaccination> vaccinations = new ArrayList<>();

    @Column(name = "mother_id")
    private Long motherId;

    @Column(name = "father_id")
    private Long fatherId;

    @Column(name = "pair_id")
    private Long pairId;

    @Column(name = "hatch_result_id")
    private Long hatchResultId;

    @Column(name = "egg_batch_id")
    private Long eggBatchId;

    @Lob
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
