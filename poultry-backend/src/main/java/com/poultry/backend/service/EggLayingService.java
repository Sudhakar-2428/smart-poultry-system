package com.poultry.backend.service;

import com.poultry.backend.dto.EggCollectionDTOs.EggCollectionResponse;
import com.poultry.backend.dto.EggLayingDTOs.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EggLayingService {
    EggLayingDashboardStats getDashboardStats();
    Page<EggLayingItemResponse> getActiveLayingHens(Pageable pageable);
    Page<EggLayingHistoryResponse> getLayingHistory(Pageable pageable);
    EggCollectionResponse startEggCollection(Long pairId);
}
