package com.poultry.backend.service;

import com.poultry.backend.dto.EggCollectionDTOs.*;
import com.poultry.backend.entity.BreedingPair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EggCollectionService {
    EggCollectionResponse createEggCollectionFromPairing(BreedingPair pair);
    Page<EggCollectionResponse> getActiveLayingHens(Pageable pageable);
    EggCollectionResponse getCollectionById(Long id);
    HenLayingProfileResponse getHenLayingProfile(Long henId);
    
    EggCollectionResponse recordDailyEggs(DailyEggRecordRequest request);
    
    Page<EggItemResponse> getEggItems(String category, String breed, String purpose, Pageable pageable);
    EggItemResponse updateEggPurpose(UpdateEggPurposeRequest request);
    
    int sendEggsToHatching(SendToHatchingRequest request);
    
    DashboardStatsResponse getDashboardStats();
    List<BatchSummaryResponse> getHenBatchHistory(Long henId);
}
