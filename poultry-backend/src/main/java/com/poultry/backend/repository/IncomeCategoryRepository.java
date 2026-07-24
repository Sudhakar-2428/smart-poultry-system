package com.poultry.backend.repository;

import com.poultry.backend.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long>, JpaSpecificationExecutor<IncomeCategory> {
    boolean existsByCategoryCode(String categoryCode);
    boolean existsByCategoryCodeAndIdNot(String categoryCode, Long id);
    java.util.Optional<IncomeCategory> findByCategoryCode(String categoryCode);
}
