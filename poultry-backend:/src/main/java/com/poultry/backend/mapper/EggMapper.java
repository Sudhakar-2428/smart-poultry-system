package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.EggBatch;
import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class EggMapper {

    /**
     * Map EggRecord to EggRecordResponse.
     */
    public EggRecordResponse toRecordResponse(EggRecord record) {
        if (record == null) {
            return null;
        }

        Integer numberOfEggs = record.getNumberOfEggs() != null ? record.getNumberOfEggs() : 0;
        Integer damagedEggs = record.getDamagedEggs() != null ? record.getDamagedEggs() : 0;
        int goodEggs = Math.max(0, numberOfEggs - damagedEggs);

        return EggRecordResponse.builder()
                .id(record.getId())
                .recordDate(record.getRecordDate())
                .henId(record.getHen() != null ? record.getHen().getId() : null)
                .henCode(record.getHen() != null ? record.getHen().getChickenCode() : null)
                .numberOfEggs(numberOfEggs)
                .damagedEggs(damagedEggs)
                .goodEggs(goodEggs)
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    /**
     * Map EggRecordRequest to EggRecord.
     */
    public EggRecord toRecordEntity(EggRecordRequest request) {
        if (request == null) {
            return null;
        }

        return EggRecord.builder()
                .recordDate(request.getRecordDate())
                .numberOfEggs(request.getNumberOfEggs())
                .damagedEggs(request.getDamagedEggs())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Map EggBatch to EggBatchResponse.
     */
    public EggBatchResponse toBatchResponse(EggBatch batch) {
        if (batch == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate expected = batch.getExpectedHatchDate();
        LocalDate batchDate = batch.getBatchDate();

        // Days remaining logic: 0 if completed or negative
        long daysRemaining = 0;
        if (expected != null
                && batch.getStatus() != EggBatchStatus.HATCHED
                && batch.getStatus() != EggBatchStatus.FAILED
                && batch.getStatus() != EggBatchStatus.SOLD) {
            daysRemaining = ChronoUnit.DAYS.between(now, expected);
        }
        daysRemaining = Math.max(0, daysRemaining);

        // Batch age logic
        long batchAge = batchDate != null ? ChronoUnit.DAYS.between(batchDate, now) : 0;
        batchAge = Math.max(0, batchAge);

        // Hatch percentage check: HATCHED computes good/total ratio, FAILED computes 0.0
        Double hatchPercentage = null;
        if (batch.getStatus() == EggBatchStatus.HATCHED) {
            int total = batch.getTotalEggs() != null ? batch.getTotalEggs() : 1;
            int good = batch.getGoodEggs() != null ? batch.getGoodEggs() : 0;
            hatchPercentage = ((double) good / total) * 100.0;
        } else if (batch.getStatus() == EggBatchStatus.FAILED) {
            hatchPercentage = 0.0;
        }

        return EggBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchDate(batchDate)
                .sourceHenId(batch.getSourceHen() != null ? batch.getSourceHen().getId() : null)
                .sourceHenCode(batch.getSourceHen() != null ? batch.getSourceHen().getChickenCode() : null)
                .totalEggs(batch.getTotalEggs())
                .goodEggs(batch.getGoodEggs())
                .damagedEggs(batch.getDamagedEggs())
                .status(batch.getStatus())
                .purpose(batch.getPurpose())
                .expectedHatchDate(expected)
                .actualHatchDate(batch.getActualHatchDate())
                .daysRemaining(daysRemaining)
                .batchAge(batchAge)
                .hatchPercentage(hatchPercentage)
                .remarks(batch.getRemarks())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    /**
     * Map EggBatch into EggBatchSummaryResponse.
     */
    public EggBatchSummaryResponse toBatchSummaryResponse(EggBatch batch) {
        if (batch == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate expected = batch.getExpectedHatchDate();
        LocalDate batchDate = batch.getBatchDate();

        long daysRemaining = 0;
        if (expected != null
                && batch.getStatus() != EggBatchStatus.HATCHED
                && batch.getStatus() != EggBatchStatus.FAILED
                && batch.getStatus() != EggBatchStatus.SOLD) {
            daysRemaining = ChronoUnit.DAYS.between(now, expected);
        }
        daysRemaining = Math.max(0, daysRemaining);

        long batchAge = batchDate != null ? ChronoUnit.DAYS.between(batchDate, now) : 0;
        batchAge = Math.max(0, batchAge);

        return EggBatchSummaryResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchDate(batchDate)
                .totalEggs(batch.getTotalEggs())
                .goodEggs(batch.getGoodEggs())
                .damagedEggs(batch.getDamagedEggs())
                .status(batch.getStatus())
                .purpose(batch.getPurpose())
                .expectedHatchDate(expected)
                .daysRemaining(daysRemaining)
                .batchAge(batchAge)
                .build();
    }

    /**
     * Map EggBatchRequest to EggBatch entity.
     */
    public EggBatch toBatchEntity(EggBatchRequest request) {
        if (request == null) {
            return null;
        }

        int total = request.getTotalEggs() != null ? request.getTotalEggs() : 0;
        int damaged = request.getDamagedEggs() != null ? request.getDamagedEggs() : 0;
        int good = Math.max(0, total - damaged);

        return EggBatch.builder()
                .batchCode(request.getBatchCode())
                .batchDate(request.getBatchDate())
                .totalEggs(total)
                .damagedEggs(damaged)
                .goodEggs(good)
                .status(request.getStatus())
                .purpose(request.getPurpose())
                .actualHatchDate(request.getActualHatchDate())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Update EggBatch entity from Request.
     */
    public void updateBatchEntityFromRequest(EggBatchRequest request, EggBatch batch) {
        if (request == null || batch == null) {
            return;
        }

        int total = request.getTotalEggs() != null ? request.getTotalEggs() : 0;
        int damaged = request.getDamagedEggs() != null ? request.getDamagedEggs() : 0;
        int good = Math.max(0, total - damaged);

        batch.setBatchCode(request.getBatchCode());
        batch.setBatchDate(request.getBatchDate());
        batch.setTotalEggs(total);
        batch.setDamagedEggs(damaged);
        batch.setGoodEggs(good);
        batch.setStatus(request.getStatus());
        batch.setPurpose(request.getPurpose());
        batch.setActualHatchDate(request.getActualHatchDate());
        batch.setRemarks(request.getRemarks());
    }
}
