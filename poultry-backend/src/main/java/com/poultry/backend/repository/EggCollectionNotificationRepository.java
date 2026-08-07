package com.poultry.backend.repository;

import com.poultry.backend.entity.EggCollectionNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EggCollectionNotificationRepository extends JpaRepository<EggCollectionNotification, Long> {
    List<EggCollectionNotification> findByNotificationDate(LocalDate date);
    List<EggCollectionNotification> findByStatus(String status);
    List<EggCollectionNotification> findByChickenIdAndNotificationDate(Long chickenId, LocalDate date);
    Optional<EggCollectionNotification> findFirstByChickenIdAndNotificationDate(Long chickenId, LocalDate date);

    @Query("SELECT e FROM EggCollectionNotification e WHERE e.status = 'PENDING' OR e.status = 'ESCALATED' ORDER BY e.createdAt DESC")
    List<EggCollectionNotification> findAllActivePending();
}
