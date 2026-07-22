package com.poultry.backend.repository;

import com.poultry.backend.entity.FeedSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedSupplierRepository extends JpaRepository<FeedSupplier, Long>, JpaSpecificationExecutor<FeedSupplier> {
    boolean existsBySupplierCode(String supplierCode);
    boolean existsBySupplierCodeAndIdNot(String supplierCode, Long id);
}
