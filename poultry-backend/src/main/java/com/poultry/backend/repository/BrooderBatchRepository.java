package com.poultry.backend.repository;

import com.poultry.backend.entity.BrooderBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrooderBatchRepository extends JpaRepository<BrooderBatch, Long>, JpaSpecificationExecutor<BrooderBatch> {
    Optional<BrooderBatch> findByBrooderCode(String brooderCode);
    boolean existsByBrooderCode(String brooderCode);
}
