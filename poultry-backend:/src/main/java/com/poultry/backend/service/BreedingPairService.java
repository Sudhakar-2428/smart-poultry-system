package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BreedingPairService {

    BreedingPairResponse createPair(BreedingPairRequest request);

    BreedingPairResponse getPairById(Long id);

    BreedingPairResponse updatePair(Long id, BreedingPairRequest request);

    BreedingPairResponse updatePairStatus(Long id, PairStatusUpdateRequest request);

    void deletePair(Long id);

    Page<BreedingPairSummaryResponse> searchPairs(
            Long maleChickenId,
            Long femaleChickenId,
            PairStatus status,
            BreedingPurpose purpose,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
