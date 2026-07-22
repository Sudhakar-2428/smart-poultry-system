package com.poultry.backend.repository;

import com.poultry.backend.entity.BreedingPair;
import com.poultry.backend.entity.PairStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreedingPairRepository extends JpaRepository<BreedingPair, Long>, JpaSpecificationExecutor<BreedingPair> {
    boolean existsByPairCode(String pairCode);
    boolean existsByPairCodeAndIdNot(String pairCode, Long id);
    
    boolean existsByMaleChickenIdAndStatus(Long maleChickenId, PairStatus status);
    boolean existsByFemaleChickenIdAndStatus(Long femaleChickenId, PairStatus status);

    boolean existsByMaleChickenIdAndStatusAndIdNot(Long maleChickenId, PairStatus status, Long id);
    boolean existsByFemaleChickenIdAndStatusAndIdNot(Long femaleChickenId, PairStatus status, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT bp FROM BreedingPair bp WHERE (bp.maleChicken.id = :id OR bp.femaleChicken.id = :id) AND bp.status = :status")
    List<BreedingPair> findByChickenIdAndStatus(@org.springframework.data.repository.query.Param("id") Long chickenId, @org.springframework.data.repository.query.Param("status") PairStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(bp) FROM BreedingPair bp WHERE bp.startDate <= :end AND (bp.endDate IS NULL OR bp.endDate >= :start)")
    long countBreedingPairsInRange(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start, @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(bp) FROM BreedingPair bp WHERE bp.status = com.poultry.backend.entity.PairStatus.ACTIVE AND bp.startDate <= :end AND (bp.endDate IS NULL OR bp.endDate >= :start)")
    long countActiveBreedingPairsInRange(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start, @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(bp.expectedEggProduction), 0) FROM BreedingPair bp WHERE bp.startDate <= :end AND (bp.endDate IS NULL OR bp.endDate >= :start)")
    long sumExpectedEggProductionInRange(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start, @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

}
