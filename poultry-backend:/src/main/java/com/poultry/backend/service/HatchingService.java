package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.BrooderStatus;
import com.poultry.backend.entity.IncubatorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface HatchingService {

    // Incubator methods
    IncubatorResponse createIncubator(IncubatorRequest request);
    IncubatorResponse getIncubatorById(Long id);
    IncubatorResponse updateIncubator(Long id, IncubatorRequest request);
    IncubatorResponse changeIncubatorStatus(Long id, IncubatorStatusRequest request);
    Page<IncubatorResponse> searchIncubators(
            String batchCode,
            IncubatorStatus status,
            LocalDate expectedHatchDate,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    // Hatch Result methods
    HatchResultResponse saveHatchResult(HatchResultRequest request);
    HatchResultResponse getHatchResultById(Long id);
    Page<HatchResultResponse> searchHatchResults(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    // Brooder methods
    Page<BrooderResponse> searchBrooders(
            String brooderCode,
            BrooderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
    BrooderResponse getBrooderById(Long id);
    BrooderResponse completeBrooder(Long id);
}
