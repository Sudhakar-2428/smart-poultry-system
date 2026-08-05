package com.poultry.backend.repository;

import com.poultry.backend.entity.EggItem;
import com.poultry.backend.entity.EggItemStatus;
import com.poultry.backend.entity.EggPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EggItemRepository extends JpaRepository<EggItem, Long>, JpaSpecificationExecutor<EggItem> {
    List<EggItem> findByFemaleChickenId(Long henId);
    List<EggItem> findByFemaleChickenIdAndBatchNumber(Long henId, Integer batchNumber);
    List<EggItem> findByIdIn(List<Long> ids);
    
    @Query("SELECT COUNT(e) FROM EggItem e WHERE e.collectionDate = :date")
    long countByCollectionDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(e) FROM EggItem e WHERE e.collectionDate >= :startDate AND e.collectionDate <= :endDate")
    long countByCollectionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(e) FROM EggItem e WHERE e.purpose = :purpose")
    long countByPurpose(@Param("purpose") EggPurpose purpose);

    @Query("SELECT COUNT(e) FROM EggItem e WHERE e.purpose IN (:purposes)")
    long countByPurposeIn(@Param("purposes") List<EggPurpose> purposes);
}
