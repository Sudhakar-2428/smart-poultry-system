package com.poultry.backend.repository;

import com.poultry.backend.entity.EggBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EggBatchRepository extends JpaRepository<EggBatch, Long>, JpaSpecificationExecutor<EggBatch> {
    Optional<EggBatch> findByBatchCode(String batchCode);
    boolean existsByBatchCode(String batchCode);

    @Query("SELECT COALESCE(SUM(b.goodEggs), 0) FROM EggBatch b WHERE b.status = com.poultry.backend.entity.EggBatchStatus.CREATED")
    long sumEggsAvailable();

    @Query("SELECT COALESCE(SUM(b.goodEggs), 0) FROM EggBatch b WHERE b.status = com.poultry.backend.entity.EggBatchStatus.SOLD")
    long sumEggsSold();

    @Query("SELECT COALESCE(SUM(b.goodEggs), 0) FROM EggBatch b WHERE b.batchDate >= :start AND b.batchDate <= :end AND b.status = com.poultry.backend.entity.EggBatchStatus.SOLD")
    long sumEggsSoldInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

