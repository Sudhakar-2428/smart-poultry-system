package com.poultry.backend.repository;

import com.poultry.backend.entity.HatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface HatchResultRepository extends JpaRepository<HatchResult, Long>, JpaSpecificationExecutor<HatchResult> {

    @Query("SELECT COALESCE(SUM(h.totalEggs), 0) FROM HatchResult h WHERE h.recordedDate >= :start AND h.recordedDate <= :end")
    long sumTotalEggsInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(h.hatchedChicks), 0) FROM HatchResult h WHERE h.recordedDate >= :start AND h.recordedDate <= :end")
    long sumHatchedChicksInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(h.unhatchedEggs + h.deadEmbryos), 0) FROM HatchResult h WHERE h.recordedDate >= :start AND h.recordedDate <= :end")
    long sumFailedEggsInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

