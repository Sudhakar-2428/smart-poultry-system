package com.poultry.backend.service;

import com.poultry.backend.dto.ChickenDashboardStatsResponse;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenOrigin;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChickenService {
    String generateNextChickenCode();
    ChickenResponse createChicken(ChickenRequest request);
    ChickenResponse getChickenById(Long id);
    ChickenResponse updateChicken(Long id, ChickenRequest request);
    ChickenResponse updateStatus(Long id, com.poultry.backend.dto.ChickenStatusPatchRequest request);
    List<com.poultry.backend.dto.ChickenTimelineEventDTO> getChickenTimeline(Long id);
    void deleteChicken(Long id);
    ChickenDashboardStatsResponse getDashboardStats();
    void bulkArchive(List<Long> ids);
    Page<ChickenSummaryResponse> searchChickens(
            String search,
            Breed breed,
            Gender gender,
            ChickenCategory category,
            ChickenStatus status,
            HealthStatus healthStatus,
            ChickenOrigin origin,
            String ageGroup,
            Integer minAgeDays,
            Integer maxAgeDays,
            Double minWeight,
            Double maxWeight,
            String chickenCode,
            String name,
            Pageable pageable
    );
}

