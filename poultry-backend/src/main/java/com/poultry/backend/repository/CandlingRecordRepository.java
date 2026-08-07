package com.poultry.backend.repository;

import com.poultry.backend.entity.CandlingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandlingRecordRepository extends JpaRepository<CandlingRecord, Long>, JpaSpecificationExecutor<CandlingRecord> {
    List<CandlingRecord> findByIncubatorBatchIdOrderByCandlingDayAsc(Long incubatorBatchId);
}
