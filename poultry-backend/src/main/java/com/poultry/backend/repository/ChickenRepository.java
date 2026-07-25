package com.poultry.backend.repository;

import com.poultry.backend.entity.Chicken;
import com.poultry.backend.entity.ChickenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface ChickenRepository extends JpaRepository<Chicken, Long>, JpaSpecificationExecutor<Chicken> {
    Optional<Chicken> findByChickenCode(String chickenCode);
    boolean existsByChickenCode(String chickenCode);
    java.util.List<Chicken> findByHatchResultId(Long hatchResultId);
    long countByStatus(ChickenStatus status);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM Chicken c")
    Long getMaxChickenId();
}

