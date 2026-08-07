package com.poultry.backend.repository;

import com.poultry.backend.entity.ChickenTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChickenTimelineRepository extends JpaRepository<ChickenTimelineEvent, Long> {
    List<ChickenTimelineEvent> findByChickenIdOrderByTimestampDesc(Long chickenId);
    List<ChickenTimelineEvent> findAllByOrderByTimestampDesc();
}
