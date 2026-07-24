package com.poultry.backend.repository;

import com.poultry.backend.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long>, JpaSpecificationExecutor<SalesOrderItem> {
    List<SalesOrderItem> findByChickenId(Long chickenId);
    List<SalesOrderItem> findByEggBatchId(Long eggBatchId);
}
