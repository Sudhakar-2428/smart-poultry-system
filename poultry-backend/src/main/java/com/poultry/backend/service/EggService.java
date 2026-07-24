package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface EggService {

    // Egg Records
    EggRecordResponse recordDailyEggs(EggRecordRequest request);
    EggRecordResponse getEggRecordById(Long id);
    EggRecordResponse updateEggRecord(Long id, EggRecordRequest request);
    void deleteEggRecord(Long id);
    Page<EggRecordResponse> searchEggRecords(
            Long henId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    // Egg Batches
    EggBatchResponse createEggBatch(EggBatchRequest request);
    EggBatchResponse getEggBatchById(Long id);
    EggBatchResponse updateEggBatch(Long id, EggBatchRequest request);
    EggBatchResponse changeBatchStatus(Long id, EggBatchStatusRequest request);
    void deleteEggBatch(Long id);
    Page<EggBatchSummaryResponse> searchEggBatches(
            String batchCode,
            EggBatchStatus status,
            EggPurpose purpose,
            LocalDate expectedHatchDate,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
