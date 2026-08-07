package com.poultry.backend.repository;

import com.poultry.backend.entity.Chicken;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChickenRepository extends JpaRepository<Chicken, Long>, JpaSpecificationExecutor<Chicken> {
    Optional<Chicken> findByChickenCode(String chickenCode);
    boolean existsByChickenCode(String chickenCode);
    List<Chicken> findByHatchResultId(Long hatchResultId);
    long countByStatus(ChickenStatus status);
    long countByHealthStatus(HealthStatus healthStatus);
    long countByGender(Gender gender);
    long countByCategory(ChickenCategory category);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByMotherId(Long motherId);
    long countByFatherId(Long fatherId);

    boolean existsByWingTagNumber(String wingTagNumber);
    boolean existsByWingTagNumberAndIdNot(String wingTagNumber, Long id);
    boolean existsByLegBandNumber(String legBandNumber);
    boolean existsByLegBandNumberAndIdNot(String legBandNumber, Long id);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM Chicken c")
    Long getMaxChickenId();
}


