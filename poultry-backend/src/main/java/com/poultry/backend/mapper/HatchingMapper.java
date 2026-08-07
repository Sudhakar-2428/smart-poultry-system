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
                .incubationMethod(request.getIncubationMethod() != null ? request.getIncubationMethod() : IncubationMethod.INCUBATOR)
                .incubatorNumber(request.getIncubatorNumber())
                .trayNumber(request.getTrayNumber())
                .turningSchedule(request.getTurningSchedule())
                .nestLocation(request.getNestLocation())
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

        int currentDay = 1;
        double progressPct = 0.0;
        if (batch.getStartDate() != null) {
            long daysPassed = ChronoUnit.DAYS.between(batch.getStartDate(), LocalDate.now()) + 1;
            currentDay = (int) Math.max(1, Math.min(21, daysPassed));
            progressPct = Math.min(100.0, Math.max(0.0, (daysPassed / 21.0) * 100.0));
        }

        int totalEggs = batch.getEggBatch() != null ? batch.getEggBatch().getTotalEggs() : 0;

        return IncubatorResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .eggBatchId(batch.getEggBatch() != null ? batch.getEggBatch().getId() : null)
                .eggBatchCode(batch.getEggBatch() != null ? batch.getEggBatch().getBatchCode() : null)
                .sourceHenId(batch.getSourceHen() != null ? batch.getSourceHen().getId() : (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null ? batch.getEggBatch().getSourceHen().getId() : null))
                .sourceHenCode(batch.getSourceHen() != null ? batch.getSourceHen().getChickenCode() : (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null ? batch.getEggBatch().getSourceHen().getChickenCode() : null))
                .sourceHenName(batch.getSourceHen() != null ? batch.getSourceHen().getName() : (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null ? batch.getEggBatch().getSourceHen().getName() : null))
                .sourceHenBreed(batch.getSourceHen() != null ? batch.getSourceHen().getBreed().name() : (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null ? batch.getEggBatch().getSourceHen().getBreed().name() : null))
                .maleChickenId(batch.getMaleChicken() != null ? batch.getMaleChicken().getId() : (batch.getEggBatch() != null && batch.getEggBatch().getMaleChicken() != null ? batch.getEggBatch().getMaleChicken().getId() : null))
                .maleChickenCode(batch.getMaleChicken() != null ? batch.getMaleChicken().getChickenCode() : (batch.getEggBatch() != null && batch.getEggBatch().getMaleChicken() != null ? batch.getEggBatch().getMaleChicken().getChickenCode() : null))
                .maleChickenName(batch.getMaleChicken() != null ? batch.getMaleChicken().getName() : (batch.getEggBatch() != null && batch.getEggBatch().getMaleChicken() != null ? batch.getEggBatch().getMaleChicken().getName() : null))
                .breedingPairId(batch.getBreedingPair() != null ? batch.getBreedingPair().getId() : null)
                .pairingCode(batch.getBreedingPair() != null ? batch.getBreedingPair().getPairCode() : null)
                .incubationMethod(batch.getIncubationMethod() != null ? batch.getIncubationMethod() : IncubationMethod.INCUBATOR)
                .incubatorNumber(batch.getIncubatorNumber())
                .trayNumber(batch.getTrayNumber())
                .turningSchedule(batch.getTurningSchedule())
                .broodyHenId(batch.getBroodyHen() != null ? batch.getBroodyHen().getId() : null)
                .broodyHenCode(batch.getBroodyHen() != null ? batch.getBroodyHen().getChickenCode() : null)
                .broodyHenName(batch.getBroodyHen() != null ? batch.getBroodyHen().getName() : null)
                .nestLocation(batch.getNestLocation())
                .totalEggs(totalEggs)
                .currentDay(currentDay)
                .progressPercentage(progressPct)
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
                .healthyChicks(request.getHealthyChicks() != null ? request.getHealthyChicks() : request.getHatchedChicks())
                .weakChicks(request.getWeakChicks() != null ? request.getWeakChicks() : 0)
                .deadChicks(request.getDeadChicks() != null ? request.getDeadChicks() : 0)
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
                .healthyChicks(result.getHealthyChicks())
                .weakChicks(result.getWeakChicks())
                .deadChicks(result.getDeadChicks())
                .deadEmbryos(result.getDeadEmbryos())
                .unhatchedEggs(result.getUnhatchedEggs())
                .hatchPercentage(result.getHatchPercentage())
                .recordedDate(result.getRecordedDate())
                .remarks(result.getRemarks())
                .build();
    }

    public CandlingRecordDTOs.CandlingRecordResponse toCandlingResponse(CandlingRecord record) {
        if (record == null) {
            return null;
        }

        return CandlingRecordDTOs.CandlingRecordResponse.builder()
                .id(record.getId())
                .incubatorBatchId(record.getIncubatorBatch() != null ? record.getIncubatorBatch().getId() : null)
                .incubatorBatchCode(record.getIncubatorBatch() != null ? record.getIncubatorBatch().getBatchCode() : null)
                .candlingDate(record.getCandlingDate())
                .candlingDay(record.getCandlingDay())
                .fertileEggs(record.getFertileEggs())
                .infertileEggs(record.getInfertileEggs())
                .deadEmbryos(record.getDeadEmbryos())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
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
