package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class HatchingMapper {

    /**
     * Map IncubatorRequest to IncubatorBatch.
     */
    public IncubatorBatch toIncubatorEntity(IncubatorRequest request) {
        if (request == null) {
            return null;
        }

        return IncubatorBatch.builder()
                .batchCode(request.getBatchCode())
                .startDate(request.getStartDate())
                .status(request.getStatus())
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .notes(request.getNotes())
                .build();
    }

    /**
     * Map IncubatorBatch to IncubatorResponse.
     */
    public IncubatorResponse toIncubatorResponse(IncubatorBatch batch) {
        if (batch == null) {
            return null;
        }

        long remainingDays = 0;
        if (batch.getExpectedHatchDate() != null && batch.getStatus() == IncubatorStatus.ACTIVE) {
            remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpectedHatchDate());
        }
        remainingDays = Math.max(0, remainingDays);

        return IncubatorResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .eggBatchId(batch.getEggBatch() != null ? batch.getEggBatch().getId() : null)
                .eggBatchCode(batch.getEggBatch() != null ? batch.getEggBatch().getBatchCode() : null)
                .startDate(batch.getStartDate())
                .expectedHatchDate(batch.getExpectedHatchDate())
                .actualHatchDate(batch.getActualHatchDate())
                .status(batch.getStatus())
                .temperature(batch.getTemperature())
                .humidity(batch.getHumidity())
                .notes(batch.getNotes())
                .remainingIncubationDays(remainingDays)
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    /**
     * Map HatchResultRequest to HatchResult.
     */
    public HatchResult toHatchEntity(HatchResultRequest request) {
        if (request == null) {
            return null;
        }

        return HatchResult.builder()
                .fertileEggs(request.getFertileEggs())
                .hatchedChicks(request.getHatchedChicks())
                .deadEmbryos(request.getDeadEmbryos())
                .recordedDate(request.getRecordedDate())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Map HatchResult to HatchResultResponse.
     */
    public HatchResultResponse toHatchResponse(HatchResult result) {
        if (result == null) {
            return null;
        }

        return HatchResultResponse.builder()
                .id(result.getId())
                .incubatorBatchId(result.getIncubatorBatch() != null ? result.getIncubatorBatch().getId() : null)
                .incubatorBatchCode(result.getIncubatorBatch() != null ? result.getIncubatorBatch().getBatchCode() : null)
                .totalEggs(result.getTotalEggs())
                .fertileEggs(result.getFertileEggs())
                .hatchedChicks(result.getHatchedChicks())
                .deadEmbryos(result.getDeadEmbryos())
                .unhatchedEggs(result.getUnhatchedEggs())
                .hatchPercentage(result.getHatchPercentage())
                .recordedDate(result.getRecordedDate())
                .remarks(result.getRemarks())
                .build();
    }

    /**
     * Map BrooderBatch to BrooderResponse.
     */
    public BrooderResponse toBrooderResponse(BrooderBatch batch) {
        if (batch == null) {
            return null;
        }

        long remainingDays = 0;
        if (batch.getExpectedEndDate() != null && batch.getStatus() == BrooderStatus.ACTIVE) {
            remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpectedEndDate());
        }
        remainingDays = Math.max(0, remainingDays);

        String batchCode = "";
        if (batch.getHatchResult() != null && batch.getHatchResult().getIncubatorBatch() != null) {
            batchCode = batch.getHatchResult().getIncubatorBatch().getBatchCode();
        }

        return BrooderResponse.builder()
                .id(batch.getId())
                .brooderCode(batch.getBrooderCode())
                .hatchResultId(batch.getHatchResult() != null ? batch.getHatchResult().getId() : null)
                .incubatorBatchCode(batchCode)
                .startDate(batch.getStartDate())
                .expectedEndDate(batch.getExpectedEndDate())
                .actualEndDate(batch.getActualEndDate())
                .status(batch.getStatus())
                .location(batch.getLocation())
                .remarks(batch.getRemarks())
                .remainingBrooderDays(remainingDays)
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
