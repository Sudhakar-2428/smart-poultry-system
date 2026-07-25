package com.poultry.backend.repository;

import com.poultry.backend.entity.HealthRecord;
import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long>, JpaSpecificationExecutor<HealthRecord> {
    
    boolean existsByRecordCode(String recordCode);
    
    boolean existsByRecordCodeAndIdNot(String recordCode, Long id);
    
    boolean existsByChickenIdAndRecordDateAndVaccinationNameAndHealthType(
            Long chickenId, LocalDate recordDate, String vaccinationName, HealthType healthType
    );
    
    boolean existsByChickenIdAndRecordDateAndVaccinationNameAndHealthTypeAndIdNot(
            Long chickenId, LocalDate recordDate, String vaccinationName, HealthType healthType, Long id
    );

    long countByHealthType(HealthType healthType);

    long countByHealthStatus(HealthStatus status);

    long countByMortalityTrue();

    long countByRecordDate(LocalDate date);

    long countByNextVaccinationDateBetween(LocalDate startDate, LocalDate endDate);

    long countByNextVaccinationDateBefore(LocalDate date);

    long countByFollowUpDateIsNotNull();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(h) FROM HealthRecord h WHERE h.nextVaccinationDate >= :date")
    long countUpcomingVaccinations(@org.springframework.data.repository.query.Param("date") LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(h) FROM HealthRecord h WHERE h.healthStatus = com.poultry.backend.entity.HealthStatus.CRITICAL AND h.chicken.status <> com.poultry.backend.entity.ChickenStatus.DEAD")
    long countCriticalCases();

    java.util.List<HealthRecord> findByNextVaccinationDateBetweenOrderByNextVaccinationDateAsc(LocalDate startDate, LocalDate endDate);

    java.util.List<HealthRecord> findByNextVaccinationDateBeforeOrderByNextVaccinationDateAsc(LocalDate date);

    java.util.List<HealthRecord> findByFollowUpDateBetweenOrderByFollowUpDateAsc(LocalDate startDate, LocalDate endDate);
}
