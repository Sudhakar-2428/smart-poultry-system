package com.poultry.backend.service.impl;

import com.poultry.backend.dto.ChickRegistrationDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.ChickRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChickRegistrationServiceImpl implements ChickRegistrationService {

    private final IncubatorBatchRepository incubatorBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final ChickenRepository chickenRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final BreedingPairRepository breedingPairRepository;

    @Override
    @Transactional
    public ChickRegistrationDTOs.ChickRegistrationSummaryResponse registerChicksForHatchBatch(Long incubatorBatchId) {
        log.info("Starting Automatic Chick Registration for Incubator Batch ID: {}", incubatorBatchId);

        IncubatorBatch batch = incubatorBatchRepository.findById(incubatorBatchId)
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + incubatorBatchId));

        HatchResult hatchResult = hatchResultRepository.findByIncubatorBatchId(incubatorBatchId)
                .orElseThrow(() -> new NotFoundException("Hatch result not found for Incubator Batch ID: " + incubatorBatchId));

        int healthyChicksCount = hatchResult.getHealthyChicks() != null ? hatchResult.getHealthyChicks() : (hatchResult.getHatchedChicks() != null ? hatchResult.getHatchedChicks() : 0);

        if (healthyChicksCount <= 0) {
            log.info("No healthy chicks to register for Hatch Batch Code: {}", batch.getBatchCode());
            return ChickRegistrationDTOs.ChickRegistrationSummaryResponse.builder()
                    .hatchBatchCode(batch.getBatchCode())
                    .totalRegisteredChicks(0)
                    .registeredChicks(List.of())
                    .build();
        }

        Chicken mother = batch.getSourceHen() != null ? batch.getSourceHen() : (batch.getEggBatch() != null ? batch.getEggBatch().getSourceHen() : null);
        Chicken father = batch.getMaleChicken() != null ? batch.getMaleChicken() : (batch.getEggBatch() != null ? batch.getEggBatch().getMaleChicken() : null);
        BreedingPair pair = batch.getBreedingPair() != null ? batch.getBreedingPair() : (batch.getEggBatch() != null ? batch.getEggBatch().getBreedingPair() : null);
        EggBatch eggBatch = batch.getEggBatch();

        // Detect Mother Hen Code Prefix
        String motherCodeRaw = mother != null ? mother.getChickenCode() : "HEN-101";
        String motherPrefix = motherCodeRaw;
        if (motherPrefix.startsWith("HEN-")) {
            motherPrefix = motherPrefix.substring(4);
        }

        // Calculate Hatch Batch Sequence for Mother Hen
        long completedBatchesCount = 0;
        if (mother != null) {
            completedBatchesCount = incubatorBatchRepository.findAll().stream()
                    .filter(b -> b.getStatus() == IncubatorStatus.COMPLETED)
                    .filter(b -> (b.getSourceHen() != null && b.getSourceHen().getId().equals(mother.getId())) ||
                                 (b.getEggBatch() != null && b.getEggBatch().getSourceHen() != null && b.getEggBatch().getSourceHen().getId().equals(mother.getId())))
                    .count();
        }
        int hatchBatchSeq = Math.max(1, (int) completedBatchesCount);

        Breed chickBreed = mother != null && mother.getBreed() != null ? mother.getBreed() : Breed.COUNTRY_CHICKEN;
        LocalDate birthDate = hatchResult.getRecordedDate() != null ? hatchResult.getRecordedDate() : LocalDate.now();

        List<ChickRegistrationDTOs.RegisteredChickDTO> registeredDTOs = new ArrayList<>();

        for (int i = 1; i <= healthyChicksCount; i++) {
            String chickSeqStr = String.format("%03d", i);
            String intelligentCode = motherPrefix + "-" + hatchBatchSeq + "-" + chickSeqStr;

            // Prevent duplicates
            int attempt = 1;
            while (chickenRepository.existsByChickenCode(intelligentCode)) {
                intelligentCode = motherPrefix + "-" + hatchBatchSeq + "-" + String.format("%03d", i + (attempt * 100));
                attempt++;
            }

            Chicken chick = Chicken.builder()
                    .chickenCode(intelligentCode)
                    .motherId(mother != null ? mother.getId() : null)
                    .fatherId(father != null ? father.getId() : null)
                    .pairId(pair != null ? pair.getId() : null)
                    .eggBatchId(eggBatch != null ? eggBatch.getId() : null)
                    .hatchResultId(hatchResult.getId())
                    .breed(chickBreed)
                    .category(ChickenCategory.CHICK)
                    .gender(Gender.UNKNOWN)
                    .origin(ChickenOrigin.FARM_BORN)
                    .dateOfBirth(birthDate)
                    .healthStatus(HealthStatus.HEALTHY)
                    .status(ChickenStatus.ACTIVE)
                    .remarks("Automatically registered from Hatch Batch " + batch.getBatchCode())
                    .build();

            Chicken savedChick = chickenRepository.save(chick);

            String qrCodeUrl = "/flock.html?id=" + savedChick.getId();

            // Timeline Event: Chick Registered
            ChickenTimelineEvent event1 = ChickenTimelineEvent.builder()
                    .chicken(savedChick)
                    .title("Chick Registered")
                    .description("Farm-born chick registered automatically with Intelligent Code: " + savedChick.getChickenCode())
                    .eventType("CHICK_REGISTERED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event1);

            // Timeline Event: QR Generated
            ChickenTimelineEvent event2 = ChickenTimelineEvent.builder()
                    .chicken(savedChick)
                    .title("QR Generated")
                    .description("QR Code generated linking to chick profile: " + qrCodeUrl)
                    .eventType("QR_GENERATED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event2);

            // Timeline Event: Added To Farm
            ChickenTimelineEvent event3 = ChickenTimelineEvent.builder()
                    .chicken(savedChick)
                    .title("Added To Farm")
                    .description("Chick added to active farm flock inventory.")
                    .eventType("ADDED_TO_FARM")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event3);

            registeredDTOs.add(ChickRegistrationDTOs.RegisteredChickDTO.builder()
                    .id(savedChick.getId())
                    .chickenCode(savedChick.getChickenCode())
                    .motherId(mother != null ? mother.getId() : null)
                    .motherCode(mother != null ? mother.getChickenCode() : null)
                    .motherName(mother != null ? mother.getName() : null)
                    .fatherId(father != null ? father.getId() : null)
                    .fatherCode(father != null ? father.getChickenCode() : null)
                    .fatherName(father != null ? father.getName() : null)
                    .breed(savedChick.getBreed())
                    .category(savedChick.getCategory())
                    .gender(savedChick.getGender())
                    .origin(savedChick.getOrigin() != null ? savedChick.getOrigin().name() : "FARM_BORN")
                    .dateOfBirth(savedChick.getDateOfBirth())
                    .healthStatus(savedChick.getHealthStatus())
                    .status(savedChick.getStatus())
                    .hatchBatchCode(batch.getBatchCode())
                    .eggBatchCode(eggBatch != null ? eggBatch.getBatchCode() : null)
                    .pairingCode(pair != null ? pair.getPairCode() : null)
                    .qrCodeUrl(qrCodeUrl)
                    .build());
        }

        // Post Timeline Event to Mother Hen & Father Rooster
        if (mother != null) {
            ChickenTimelineEvent motherEvent = ChickenTimelineEvent.builder()
                    .chicken(mother)
                    .title("Chicks Registered")
                    .description("Registered " + healthyChicksCount + " chicks from Hatch Batch " + batch.getBatchCode() + ".")
                    .eventType("CHICKS_REGISTERED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(motherEvent);
        }

        if (father != null && (mother == null || !father.getId().equals(mother.getId()))) {
            ChickenTimelineEvent fatherEvent = ChickenTimelineEvent.builder()
                    .chicken(father)
                    .title("Chicks Registered")
                    .description("Registered " + healthyChicksCount + " chicks from Hatch Batch " + batch.getBatchCode() + ".")
                    .eventType("CHICKS_REGISTERED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(fatherEvent);
        }

        log.info("Successfully registered {} healthy chicks for Batch Code: {}", healthyChicksCount, batch.getBatchCode());

        return ChickRegistrationDTOs.ChickRegistrationSummaryResponse.builder()
                .hatchBatchCode(batch.getBatchCode())
                .totalRegisteredChicks(healthyChicksCount)
                .motherHenCode(mother != null ? mother.getChickenCode() : null)
                .fatherRoosterCode(father != null ? father.getChickenCode() : null)
                .hatchBatchSequence(hatchBatchSeq)
                .registeredChicks(registeredDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChickRegistrationDTOs.ParentChickStatsResponse getParentChickStats(Long chickenId) {
        Chicken chicken = chickenRepository.findById(chickenId)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + chickenId));

        boolean isHen = chicken.getGender() == Gender.FEMALE;

        long totalChicksProduced = isHen
                ? chickenRepository.countByMotherId(chickenId)
                : chickenRepository.countByFatherId(chickenId);

        long totalHatchBatches = incubatorBatchRepository.findAll().stream()
                .filter(b -> b.getStatus() == IncubatorStatus.COMPLETED)
                .filter(b -> (isHen && ((b.getSourceHen() != null && b.getSourceHen().getId().equals(chickenId)) ||
                                        (b.getEggBatch() != null && b.getEggBatch().getSourceHen() != null && b.getEggBatch().getSourceHen().getId().equals(chickenId)))) ||
                             (!isHen && ((b.getMaleChicken() != null && b.getMaleChicken().getId().equals(chickenId)) ||
                                         (b.getEggBatch() != null && b.getEggBatch().getMaleChicken() != null && b.getEggBatch().getMaleChicken().getId().equals(chickenId)))))
                .count();

        long partnerHensCount = 0;
        if (!isHen) {
            partnerHensCount = breedingPairRepository.findAll().stream()
                    .filter(p -> p.getMaleChicken() != null && p.getMaleChicken().getId().equals(chickenId))
                    .map(p -> p.getFemaleChicken().getId())
                    .distinct()
                    .count();
        }

        return ChickRegistrationDTOs.ParentChickStatsResponse.builder()
                .chickenId(chicken.getId())
                .chickenCode(chicken.getChickenCode())
                .name(chicken.getName())
                .gender(chicken.getGender() != null ? chicken.getGender().name() : "FEMALE")
                .totalHatchBatches(totalHatchBatches)
                .totalChicksProduced(totalChicksProduced)
                .currentHatchChicks(totalChicksProduced)
                .partnerHensCount(partnerHensCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChickRegistrationDTOs.ChickReportDTO getChickReport(String reportType, Long filterId) {
        List<Chicken> chicksList = chickenRepository.findAll().stream()
                .filter(c -> c.getCategory() == ChickenCategory.CHICK)
                .toList();

        if ("MOTHER".equalsIgnoreCase(reportType) && filterId != null) {
            chicksList = chicksList.stream().filter(c -> c.getMotherId() != null && c.getMotherId().equals(filterId)).toList();
        } else if ("FATHER".equalsIgnoreCase(reportType) && filterId != null) {
            chicksList = chicksList.stream().filter(c -> c.getFatherId() != null && c.getFatherId().equals(filterId)).toList();
        }

        List<ChickRegistrationDTOs.RegisteredChickDTO> registered = chicksList.stream().map(c -> {
            Chicken mother = c.getMotherId() != null ? chickenRepository.findById(c.getMotherId()).orElse(null) : null;
            Chicken father = c.getFatherId() != null ? chickenRepository.findById(c.getFatherId()).orElse(null) : null;

            return ChickRegistrationDTOs.RegisteredChickDTO.builder()
                    .id(c.getId())
                    .chickenCode(c.getChickenCode())
                    .motherId(c.getMotherId())
                    .motherCode(mother != null ? mother.getChickenCode() : null)
                    .motherName(mother != null ? mother.getName() : null)
                    .fatherId(c.getFatherId())
                    .fatherCode(father != null ? father.getChickenCode() : null)
                    .fatherName(father != null ? father.getName() : null)
                    .breed(c.getBreed())
                    .category(c.getCategory())
                    .gender(c.getGender())
                    .origin(c.getOrigin() != null ? c.getOrigin().name() : "FARM_BORN")
                    .dateOfBirth(c.getDateOfBirth())
                    .healthStatus(c.getHealthStatus())
                    .status(c.getStatus())
                    .qrCodeUrl("/flock.html?id=" + c.getId())
                    .build();
        }).toList();

        long healthyCount = registered.stream().filter(r -> r.getHealthStatus() == HealthStatus.HEALTHY).count();
        double healthyPct = registered.size() > 0 ? (healthyCount * 100.0 / registered.size()) : 0.0;

        return ChickRegistrationDTOs.ChickReportDTO.builder()
                .reportTitle(reportType.toUpperCase() + " Chick Registration Report")
                .groupName(reportType)
                .totalChicks((long) registered.size())
                .healthyPercentage(healthyPct)
                .chicks(registered)
                .build();
    }
}
