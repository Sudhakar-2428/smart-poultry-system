package com.poultry.backend.repository;

import com.poultry.backend.entity.IncubatorBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncubatorBatchRepository extends JpaRepository<IncubatorBatch, Long>, JpaSpecificationExecutor<IncubatorBatch> {
    Optional<IncubatorBatch> findByBatchCode(String batchCode);
    boolean existsByBatchCode(String batchCode);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM IncubatorBatch i WHERE i.status = com.poultry.backend.entity.IncubatorStatus.ACTIVE")
    long countActiveBatches();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM IncubatorBatch i WHERE i.status = com.poultry.backend.entity.IncubatorStatus.COMPLETED")
    long countCompletedBatches();

}
