package com.poultry.backend.repository;

import com.poultry.backend.entity.FeedConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedConsumptionRepository extends JpaRepository<FeedConsumption, Long>, JpaSpecificationExecutor<FeedConsumption> {
    List<FeedConsumption> findByChickenId(Long chickenId);
    List<FeedConsumption> findByBrooderBatchId(Long brooderBatchId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.quantity), 0.0) FROM FeedConsumption c WHERE c.consumptionDate = :date")
    double sumConsumptionByDate(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.quantity), 0.0) FROM FeedConsumption c WHERE c.consumptionDate >= :start AND c.consumptionDate <= :end")
    double sumConsumptionInRange(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start, @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

}
