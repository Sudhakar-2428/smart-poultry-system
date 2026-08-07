package com.poultry.backend.service.impl;

import com.poultry.backend.dto.HatchingReportDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.HatchingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HatchingReportServiceImpl implements HatchingReportService {

    private final IncubatorBatchRepository incubatorBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final CandlingRecordRepository candlingRecordRepository;
    private final HatchingReportRepository hatchingReportRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final ChickenRepository chickenRepository;

    @Override
    @Transactional
    public HatchingReportDTOs.HatchingReportResponse generateHatchingReport(Long incubatorBatchId) {
        log.info("Generating Hatching Report for Incubator Batch ID: {}", incubatorBatchId);

        IncubatorBatch batch = incubatorBatchRepository.findById(incubatorBatchId)
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + incubatorBatchId));

        HatchResult hatchResult = hatchResultRepository.findByIncubatorBatchId(incubatorBatchId)
                .orElse(null);

        List<CandlingRecord> candlingRecords = candlingRecordRepository.findByIncubatorBatchIdOrderByCandlingDayAsc(incubatorBatchId);

        Chicken mother = batch.getSourceHen() != null ? batch.getSourceHen() : (batch.getEggBatch() != null ? batch.getEggBatch().getSourceHen() : null);
        Chicken father = batch.getMaleChicken() != null ? batch.getMaleChicken() : (batch.getEggBatch() != null ? batch.getEggBatch().getMaleChicken() : null);
        BreedingPair pair = batch.getBreedingPair() != null ? batch.getBreedingPair() : (batch.getEggBatch() != null ? batch.getEggBatch().getBreedingPair() : null);
        EggBatch eggBatch = batch.getEggBatch();

        // Extract candling checks per milestone day
        CandlingRecord day7 = candlingRecords.stream().filter(c -> c.getCandlingDay() != null && c.getCandlingDay() == 7).findFirst().orElse(null);
        CandlingRecord day14 = candlingRecords.stream().filter(c -> c.getCandlingDay() != null && c.getCandlingDay() == 14).findFirst().orElse(null);
        CandlingRecord day18 = candlingRecords.stream().filter(c -> c.getCandlingDay() != null && c.getCandlingDay() == 18).findFirst().orElse(null);

        int totalEggsSet = eggBatch != null ? eggBatch.getTotalEggs() : 0;
        int fertileEggs = hatchResult != null ? hatchResult.getFertileEggs() : (day7 != null ? day7.getFertileEggs() : totalEggsSet);
        int hatchedChicks = hatchResult != null ? hatchResult.getHatchedChicks() : 0;
        int healthyChicks = hatchResult != null && hatchResult.getHealthyChicks() != null ? hatchResult.getHealthyChicks() : hatchedChicks;
        int weakChicks = hatchResult != null && hatchResult.getWeakChicks() != null ? hatchResult.getWeakChicks() : 0;
        int deadChicks = hatchResult != null && hatchResult.getDeadChicks() != null ? hatchResult.getDeadChicks() : 0;
        int deadEmbryos = hatchResult != null ? hatchResult.getDeadEmbryos() : 0;
        int unhatchedEggs = hatchResult != null ? hatchResult.getUnhatchedEggs() : Math.max(0, totalEggsSet - hatchedChicks);
        double hatchSuccessPct = hatchResult != null && hatchResult.getHatchPercentage() != null ? hatchResult.getHatchPercentage() : (totalEggsSet > 0 ? (hatchedChicks * 100.0 / totalEggsSet) : 0.0);

        // Performance Calculations
        double fertilityRate = totalEggsSet > 0 ? (fertileEggs * 100.0 / totalEggsSet) : 0.0;
        double hatchSuccessRate = totalEggsSet > 0 ? (hatchedChicks * 100.0 / totalEggsSet) : 0.0;
        double healthyChickRate = hatchedChicks > 0 ? (healthyChicks * 100.0 / hatchedChicks) : 0.0;
        double lossPercentage = totalEggsSet > 0 ? ((deadEmbryos + unhatchedEggs) * 100.0 / totalEggsSet) : 0.0;

        String reportCode = "HR-" + batch.getBatchCode();

        HatchingReport report = hatchingReportRepository.findByIncubatorBatchId(incubatorBatchId)
                .orElseGet(() -> HatchingReport.builder()
                        .incubatorBatch(batch)
                        .reportCode(reportCode)
                        .build());

        report.setReportCode(reportCode);
        report.setReportDate(LocalDateTime.now());
        report.setFarmName("Greenfield Hatchery");
        report.setGeneratedBy("System");

        // Mother Hen Details
        if (mother != null) {
            report.setMotherHenCode(mother.getChickenCode());
            report.setMotherHenName(mother.getName() != null ? mother.getName() : mother.getChickenCode());
            report.setMotherHenBreed(mother.getBreed() != null ? mother.getBreed().name() : "Country Chicken");
            report.setMotherHenAge("24 Months");
            report.setMotherHenOrigin("Farm Born");
        }

        // Father Rooster Details
        if (father != null) {
            report.setFatherRoosterCode(father.getChickenCode());
            report.setFatherRoosterName(father.getName() != null ? father.getName() : father.getChickenCode());
            report.setFatherRoosterBreed(father.getBreed() != null ? father.getBreed().name() : "Country Chicken");
            report.setFatherRoosterAge("30 Months");
            report.setFatherRoosterOrigin("Farm Born");
        }

        // Breeding Details
        report.setPairingCode(pair != null ? pair.getPairCode() : "PAIR-001");
        report.setPairingDate(pair != null ? pair.getStartDate() : LocalDate.now().minusDays(40));
        report.setEggLayingStartDate(eggBatch != null && eggBatch.getBatchDate() != null ? eggBatch.getBatchDate() : LocalDate.now().minusDays(28));

        int collDays = 14;
        if (report.getEggLayingStartDate() != null && batch.getStartDate() != null) {
            collDays = (int) Math.max(1, ChronoUnit.DAYS.between(report.getEggLayingStartDate(), batch.getStartDate()));
        }
        report.setCollectionPeriodDays(collDays);
        report.setIncubationMethod(batch.getIncubationMethod() != null ? batch.getIncubationMethod() : IncubationMethod.INCUBATOR);
        report.setEquipmentOrNest(batch.getIncubationMethod() == IncubationMethod.NATURAL_BROODING ? batch.getNestLocation() : batch.getIncubatorNumber());

        // Egg Summary
        report.setTotalEggsCollected(eggBatch != null ? eggBatch.getTotalEggs() : totalEggsSet);
        report.setEggsSelectedForHatching(totalEggsSet);
        report.setHealthyEggs(eggBatch != null && eggBatch.getGoodEggs() != null ? eggBatch.getGoodEggs() : totalEggsSet);
        report.setBrokenEggs(eggBatch != null && eggBatch.getBrokenEggs() != null ? eggBatch.getBrokenEggs() : 0);
        report.setRejectedEggs(eggBatch != null && eggBatch.getDamagedEggs() != null ? eggBatch.getDamagedEggs() : 0);

        // Candling
        report.setDay7Fertile(day7 != null ? day7.getFertileEggs() : fertileEggs);
        report.setDay7Infertile(day7 != null ? day7.getInfertileEggs() : 0);
        report.setDay7DeadEmbryos(day7 != null ? day7.getDeadEmbryos() : 0);

        report.setDay14Fertile(day14 != null ? day14.getFertileEggs() : fertileEggs);
        report.setDay14Infertile(day14 != null ? day14.getInfertileEggs() : 0);
        report.setDay14DeadEmbryos(day14 != null ? day14.getDeadEmbryos() : 0);

        report.setDay18Fertile(day18 != null ? day18.getFertileEggs() : fertileEggs);
        report.setDay18Infertile(day18 != null ? day18.getInfertileEggs() : 0);
        report.setDay18DeadEmbryos(day18 != null ? day18.getDeadEmbryos() : 0);

        // Hatch Results
        report.setTotalEggsSet(totalEggsSet);
        report.setFertileEggs(fertileEggs);
        report.setHatchedChicks(hatchedChicks);
        report.setHealthyChicks(healthyChicks);
        report.setWeakChicks(weakChicks);
        report.setDeadChicks(deadChicks);
        report.setUnhatchedEggs(unhatchedEggs);
        report.setHatchSuccessPercentage(hatchSuccessPct);

        // Performance
        report.setFertilityRate(fertilityRate);
        report.setHatchSuccessRate(hatchSuccessRate);
        report.setHealthyChickRate(healthyChickRate);
        report.setLossPercentage(lossPercentage);

        HatchingReport saved = hatchingReportRepository.save(report);

        // Post Timeline Event to Mother Hen & Father Rooster
        if (mother != null && mother.getId() != null) {
            chickenRepository.findById(mother.getId()).ifPresent(m -> {
                ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                        .chicken(m)
                        .title("Hatching Report Generated")
                        .description("Hatching Report " + saved.getReportCode() + " generated for batch " + batch.getBatchCode() + ". Success: " + String.format("%.1f", hatchSuccessPct) + "%.")
                        .eventType("HATCHING_REPORT")
                        .createdBy("System")
                        .build();
                chickenTimelineRepository.save(event);
            });
        }

        if (father != null && father.getId() != null && (mother == null || !father.getId().equals(mother.getId()))) {
            chickenRepository.findById(father.getId()).ifPresent(f -> {
                ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                        .chicken(f)
                        .title("Hatching Report Generated")
                        .description("Hatching Report " + saved.getReportCode() + " generated for batch " + batch.getBatchCode() + ". Success: " + String.format("%.1f", hatchSuccessPct) + "%.")
                        .eventType("HATCHING_REPORT")
                        .createdBy("System")
                        .build();
                chickenTimelineRepository.save(event);
            });
        }

        return toReportResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HatchingReportDTOs.HatchingReportResponse getReportByBatchId(Long incubatorBatchId) {
        HatchingReport report = hatchingReportRepository.findByIncubatorBatchId(incubatorBatchId)
                .orElseGet(() -> {
                    // Auto-generate if missing
                    return null;
                });

        if (report == null) {
            return generateHatchingReport(incubatorBatchId);
        }
        return toReportResponse(report);
    }

    private HatchingReportDTOs.HatchingReportResponse toReportResponse(HatchingReport report) {
        return HatchingReportDTOs.HatchingReportResponse.builder()
                .id(report.getId())
                .reportCode(report.getReportCode())
                .incubatorBatchId(report.getIncubatorBatch() != null ? report.getIncubatorBatch().getId() : null)
                .hatchBatchCode(report.getIncubatorBatch() != null ? report.getIncubatorBatch().getBatchCode() : null)
                .eggBatchCode(report.getIncubatorBatch() != null && report.getIncubatorBatch().getEggBatch() != null ? report.getIncubatorBatch().getEggBatch().getBatchCode() : null)
                .pairingCode(report.getPairingCode())
                .reportDate(report.getReportDate())
                .farmName(report.getFarmName())
                .generatedBy(report.getGeneratedBy())

                .motherHenCode(report.getMotherHenCode())
                .motherHenName(report.getMotherHenName())
                .motherHenBreed(report.getMotherHenBreed())
                .motherHenAge(report.getMotherHenAge())
                .motherHenOrigin(report.getMotherHenOrigin())

                .fatherRoosterCode(report.getFatherRoosterCode())
                .fatherRoosterName(report.getFatherRoosterName())
                .fatherRoosterBreed(report.getFatherRoosterBreed())
                .fatherRoosterAge(report.getFatherRoosterAge())
                .fatherRoosterOrigin(report.getFatherRoosterOrigin())

                .pairingDate(report.getPairingDate())
                .eggLayingStartDate(report.getEggLayingStartDate())
                .collectionPeriodDays(report.getCollectionPeriodDays())
                .incubationMethod(report.getIncubationMethod())
                .equipmentOrNest(report.getEquipmentOrNest())

                .totalEggsCollected(report.getTotalEggsCollected())
                .eggsSelectedForHatching(report.getEggsSelectedForHatching())
                .healthyEggs(report.getHealthyEggs())
                .brokenEggs(report.getBrokenEggs())
                .rejectedEggs(report.getRejectedEggs())

                .day7Fertile(report.getDay7Fertile())
                .day7Infertile(report.getDay7Infertile())
                .day7DeadEmbryos(report.getDay7DeadEmbryos())

                .day14Fertile(report.getDay14Fertile())
                .day14Infertile(report.getDay14Infertile())
                .day14DeadEmbryos(report.getDay14DeadEmbryos())

                .day18Fertile(report.getDay18Fertile())
                .day18Infertile(report.getDay18Infertile())
                .day18DeadEmbryos(report.getDay18DeadEmbryos())

                .totalEggsSet(report.getTotalEggsSet())
                .fertileEggs(report.getFertileEggs())
                .hatchedChicks(report.getHatchedChicks())
                .healthyChicks(report.getHealthyChicks())
                .weakChicks(report.getWeakChicks())
                .deadChicks(report.getDeadChicks())
                .unhatchedEggs(report.getUnhatchedEggs())
                .hatchSuccessPercentage(report.getHatchSuccessPercentage())

                .fertilityRate(report.getFertilityRate())
                .hatchSuccessRate(report.getHatchSuccessRate())
                .healthyChickRate(report.getHealthyChickRate())
                .lossPercentage(report.getLossPercentage())

                .createdAt(report.getCreatedAt())
                .build();
    }
}
