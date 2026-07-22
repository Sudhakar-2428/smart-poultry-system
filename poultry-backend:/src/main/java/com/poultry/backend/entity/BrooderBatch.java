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
@Table(name = "brooder_batches", uniqueConstraints = {
        @UniqueConstraint(columnNames = "brooder_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrooderBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Brooder code is required")
    @Column(name = "brooder_code", nullable = false, length = 50)
    private String brooderCode;

    @NotNull(message = "Hatch result is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hatch_result_id", nullable = false)
    private HatchResult hatchResult;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Expected end date is required")
    @Column(name = "expected_end_date", nullable = false)
    private LocalDate expectedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BrooderStatus status;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
