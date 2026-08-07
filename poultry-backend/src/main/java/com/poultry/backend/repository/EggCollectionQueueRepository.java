package com.poultry.backend.repository;

import com.poultry.backend.entity.EggCollectionQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EggCollectionQueueRepository extends JpaRepository<EggCollectionQueueItem, Long> {
    List<EggCollectionQueueItem> findByQueueDate(LocalDate queueDate);
    Optional<EggCollectionQueueItem> findByQueueDateAndChickenId(LocalDate queueDate, Long chickenId);
    List<EggCollectionQueueItem> findByQueueDateAndStatus(LocalDate queueDate, String status);
}
