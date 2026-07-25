package com.poultry.backend.service;

import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChickenService {
    String generateNextChickenCode();
    ChickenResponse createChicken(ChickenRequest request);
    ChickenResponse getChickenById(Long id);
    ChickenResponse updateChicken(Long id, ChickenRequest request);
    void deleteChicken(Long id);
    Page<ChickenSummaryResponse> searchChickens(
            Breed breed,
            Gender gender,
            ChickenCategory category,
            ChickenStatus status,
            Integer minAgeDays,
            Integer maxAgeDays,
            Double minWeight,
            Double maxWeight,
            String chickenCode,
            String name,
            Pageable pageable
    );
}
