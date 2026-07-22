package com.poultry.backend.repository;

import com.poultry.backend.entity.ChickGrowthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ChickGrowthRecordRepository extends JpaRepository<ChickGrowthRecord, Long>, JpaSpecificationExecutor<ChickGrowthRecord> {
    boolean existsByChickenIdAndGrowthDate(Long chickenId, LocalDate growthDate);
    boolean existsByChickenIdAndGrowthDateAndIdNot(Long chickenId, LocalDate growthDate, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.weight) FROM ChickGrowthRecord r WHERE r.growthDate >= :start AND r.growthDate <= :end")
    Double getAverageWeightInRange(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.height) FROM ChickGrowthRecord r WHERE r.growthDate >= :start AND r.growthDate <= :end")
    Double getAverageHeightInRange(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM ChickGrowthRecord r WHERE r.growthDate >= :start AND r.growthDate <= :end")
    long countByGrowthDateInRange(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

}
