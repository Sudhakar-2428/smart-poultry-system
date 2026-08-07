package com.poultry.backend.repository;

import com.poultry.backend.entity.HatchingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HatchingReportRepository extends JpaRepository<HatchingReport, Long>, JpaSpecificationExecutor<HatchingReport> {
    Optional<HatchingReport> findByIncubatorBatchId(Long incubatorBatchId);
    Optional<HatchingReport> findByReportCode(String reportCode);
}
