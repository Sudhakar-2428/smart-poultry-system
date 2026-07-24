package com.poultry.backend.repository;

import com.poultry.backend.entity.FeedPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedPurchaseRepository extends JpaRepository<FeedPurchase, Long>, JpaSpecificationExecutor<FeedPurchase> {
    boolean existsByPurchaseCode(String purchaseCode);
    List<FeedPurchase> findBySupplierId(Long supplierId);
}
