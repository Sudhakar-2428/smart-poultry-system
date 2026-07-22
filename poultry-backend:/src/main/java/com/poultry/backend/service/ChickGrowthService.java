package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.GrowthStage;
import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ChickGrowthService {

    ChickGrowthResponse createGrowthRecord(ChickGrowthRequest request);

    ChickGrowthResponse getGrowthRecordById(Long id);

    ChickGrowthResponse updateGrowthRecord(Long id, ChickGrowthRequest request);

    void deleteGrowthRecord(Long id);

    Page<ChickGrowthResponse> searchGrowthRecords(
            GrowthStage growthStage,
            Gender gender,
            HealthStatus healthStatus,
            Integer minAge,
            Integer maxAge,
            Double minWeight,
            Double maxWeight,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    ChickenResponse updateChickenGender(Long id, GenderUpdateRequest request);

    ChickenResponse completeAdultTransition(Long id, AdultTransitionRequest request);
}
