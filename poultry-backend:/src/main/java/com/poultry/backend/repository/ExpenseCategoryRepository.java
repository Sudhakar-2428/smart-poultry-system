package com.poultry.backend.repository;

import com.poultry.backend.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long>, JpaSpecificationExecutor<ExpenseCategory> {
    boolean existsByCategoryCode(String categoryCode);
    boolean existsByCategoryCodeAndIdNot(String categoryCode, Long id);
    java.util.Optional<ExpenseCategory> findByCategoryCode(String categoryCode);
}
