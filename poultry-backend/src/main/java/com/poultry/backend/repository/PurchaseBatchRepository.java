package com.poultry.backend.repository;

import com.poultry.backend.entity.PurchaseBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseBatchRepository extends JpaRepository<PurchaseBatch, Long> {
    Optional<PurchaseBatch> findByBatchCode(String batchCode);
    boolean existsByBatchCode(String batchCode);
    List<PurchaseBatch> findBySupplierNameContainingIgnoreCase(String supplierName);

    @Query("SELECT COALESCE(MAX(pb.id), 0) FROM PurchaseBatch pb")
    Long getMaxPurchaseBatchId();
}
