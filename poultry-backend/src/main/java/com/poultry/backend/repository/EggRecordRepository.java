package com.poultry.backend.repository;

import com.poultry.backend.entity.EggRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EggRecordRepository extends JpaRepository<EggRecord, Long>, JpaSpecificationExecutor<EggRecord> {

    @Query("SELECT COALESCE(SUM(r.numberOfEggs), 0) FROM EggRecord r")
    long sumTotalEggsProduced();

    @Query("SELECT COALESCE(SUM(r.numberOfEggs), 0) FROM EggRecord r WHERE r.recordDate >= :start AND r.recordDate <= :end")
    long sumTotalEggsProducedInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.damagedEggs), 0) FROM EggRecord r WHERE r.recordDate >= :start AND r.recordDate <= :end")
    long sumDamagedEggsInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

