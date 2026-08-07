package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.HatchingMapper;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.HatchingReportService;
import com.poultry.backend.service.HatchingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HatchingServiceImpl implements HatchingService {

    private final IncubatorBatchRepository incubatorBatchRepository;
    private final HatchResultRepository hatchResultRepository;
    private final BrooderBatchRepository brooderBatchRepository;
    private final EggBatchRepository eggBatchRepository;
    private final ChickenRepository chickenRepository;
    private final FarmSettingRepository farmSettingRepository;
    private final CandlingRecordRepository candlingRecordRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;
    private final HatchingReportService hatchingReportService;
    private final HatchingMapper hatchingMapper;

    @Override
    @Transactional
    public IncubatorResponse createIncubator(IncubatorRequest request) {
        log.info("Creating incubator batch. Code: {}, Egg Batch ID: {}", request.getBatchCode(), request.getEggBatchId());

        EggBatch eggBatch = eggBatchRepository.findById(request.getEggBatchId())
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + request.getEggBatchId()));

        if (eggBatch.getPurpose() != null && eggBatch.getPurpose() != EggPurpose.HATCHING) {
            throw new ValidationException("Egg batch purpose must be HATCHING to create an incubator batch.");
        }
        if (eggBatch.getStatus() == EggBatchStatus.HATCHED) {
            throw new ValidationException("Egg batch status must be CREATED or BROODING to start incubation.");
        }

        String batchCode = request.getBatchCode();
        if (batchCode == null || batchCode.trim().isEmpty() || batchCode.startsWith("IB-")) {
            batchCode = "HB-2026-" + String.format("%03d", System.currentTimeMillis() % 1000);
        }

        if (incubatorBatchRepository.existsByBatchCode(batchCode)) {
            batchCode = "HB-2026-" + String.format("%03d", (System.currentTimeMillis() + 17) % 1000);
        }

        IncubatorBatch incubatorBatch = hatchingMapper.toIncubatorEntity(request);
        incubatorBatch.setBatchCode(batchCode);
        incubatorBatch.setEggBatch(eggBatch);
        incubatorBatch.setSourceHen(eggBatch.getSourceHen());
        incubatorBatch.setMaleChicken(eggBatch.getMaleChicken());
        incubatorBatch.setBreedingPair(eggBatch.getBreedingPair());
        incubatorBatch.setExpectedHatchDate(eggBatch.getExpectedHatchDate() != null ? eggBatch.getExpectedHatchDate() : request.getStartDate().plusDays(21));

        IncubatorBatch savedBatch = incubatorBatchRepository.save(incubatorBatch);

        // Update Egg Batch status to INCUBATING
        eggBatch.setStatus(EggBatchStatus.INCUBATING);
        eggBatchRepository.save(eggBatch);

        // Record Timeline Event
        if (eggBatch.getSourceHen() != null) {
            ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                    .chicken(eggBatch.getSourceHen())
                    .title("Incubation Started")
                    .description("Batch " + savedBatch.getBatchCode() + " started incubation with " + eggBatch.getTotalEggs() + " eggs.")
                    .eventType("INCUBATION_STARTED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event);
        }

        log.info("AUDIT: Incubation Started. Incubator Batch Code: {}, Egg Batch ID: {}",
                savedBatch.getBatchCode(), eggBatch.getId());

        return hatchingMapper.toIncubatorResponse(savedBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public HatchingDashboardStats getDashboardStats() {
        List<IncubatorBatch> allBatches = incubatorBatchRepository.findAll();
        List<IncubatorBatch> activeBatches = allBatches.stream()
                .filter(b -> b.getStatus() == IncubatorStatus.ACTIVE)
                .toList();

        long activeCount = activeBatches.size();
        long eggsUnderIncubation = activeBatches.stream()
                .mapToLong(b -> b.getEggBatch() != null ? b.getEggBatch().getTotalEggs() : 0)
                .sum();

        LocalDate today = LocalDate.now();
        long expectedToday = activeBatches.stream()
                .filter(b -> b.getExpectedHatchDate() != null && b.getExpectedHatchDate().equals(today))
                .count();

        List<HatchResult> results = hatchResultRepository.findAll();
        long totalHatched = results.stream().mapToLong(r -> r.getHatchedChicks() != null ? r.getHatchedChicks() : 0).sum();
        long totalFailed = results.stream().mapToLong(r -> (r.getDeadEmbryos() != null ? r.getDeadEmbryos() : 0) + (r.getUnhatchedEggs() != null ? r.getUnhatchedEggs() : 0)).sum();

        double avgSuccessRate = 0.0;
        if (!results.isEmpty()) {
            avgSuccessRate = results.stream().mapToDouble(r -> r.getHatchPercentage() != null ? r.getHatchPercentage() : 0.0).average().orElse(0.0);
        }

        return HatchingDashboardStats.builder()
                .activeHatchBatches(activeCount)
                .eggsUnderIncubation(eggsUnderIncubation)
                .expectedHatchToday(expectedToday)
                .successfullyHatched(totalHatched)
                .failedEggs(totalFailed)
                .hatchSuccessRate(avgSuccessRate)
                .build();
    }

    @Override
    @Transactional
    public CandlingRecordDTOs.CandlingRecordResponse recordCandling(CandlingRecordDTOs.CandlingRecordRequest request) {
        IncubatorBatch batch = incubatorBatchRepository.findById(request.getIncubatorBatchId())
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + request.getIncubatorBatchId()));

        CandlingRecord record = CandlingRecord.builder()
                .incubatorBatch(batch)
                .candlingDate(request.getCandlingDate())
                .candlingDay(request.getCandlingDay())
                .fertileEggs(request.getFertileEggs())
                .infertileEggs(request.getInfertileEggs())
                .deadEmbryos(request.getDeadEmbryos())
                .remarks(request.getRemarks())
                .build();

        CandlingRecord saved = candlingRecordRepository.save(record);

        // Record Timeline Event
        if (batch.getSourceHen() != null) {
            ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                    .chicken(batch.getSourceHen())
                    .title("Candling Recorded - Day " + request.getCandlingDay())
                    .description("Candling check for " + batch.getBatchCode() + ": " + request.getFertileEggs() + " fertile, " + request.getInfertileEggs() + " infertile, " + request.getDeadEmbryos() + " dead embryos.")
                    .eventType("CANDLING_CHECK")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event);
        }

        return hatchingMapper.toCandlingResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandlingRecordDTOs.CandlingRecordResponse> getCandlingRecords(Long batchId) {
        return candlingRecordRepository.findByIncubatorBatchIdOrderByCandlingDayAsc(batchId).stream()
                .map(hatchingMapper::toCandlingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IncubatorResponse getIncubatorById(Long id) {
        log.info("Fetching incubator details for ID: {}", id);
        IncubatorBatch batch = incubatorBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + id));
        return hatchingMapper.toIncubatorResponse(batch);
    }

    @Override
    @Transactional
    public IncubatorResponse updateIncubator(Long id, IncubatorRequest request) {
        log.info("Updating incubator batch ID: {}", id);

        IncubatorBatch batch = incubatorBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + id));

        if (!batch.getBatchCode().equalsIgnoreCase(request.getBatchCode())) {
            if (incubatorBatchRepository.existsByBatchCode(request.getBatchCode())) {
                throw new DuplicateRecordException("Incubator batch code '" + request.getBatchCode() + "' is already registered.");
            }
        }

        EggBatch eggBatch = eggBatchRepository.findById(request.getEggBatchId())
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + request.getEggBatchId()));

        batch.setBatchCode(request.getBatchCode());
        batch.setEggBatch(eggBatch);
        batch.setStartDate(request.getStartDate());
        batch.setStatus(request.getStatus());
        batch.setTemperature(request.getTemperature());
        batch.setHumidity(request.getHumidity());
        batch.setNotes(request.getNotes());

        IncubatorBatch updated = incubatorBatchRepository.save(batch);
        return hatchingMapper.toIncubatorResponse(updated);
    }

    @Override
    @Transactional
    public IncubatorResponse changeIncubatorStatus(Long id, IncubatorStatusRequest request) {
        log.info("Changing incubator status for ID: {} to {}", id, request.getStatus());

        IncubatorBatch batch = incubatorBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + id));

        batch.setStatus(request.getStatus());
        if (request.getActualHatchDate() != null) {
            batch.setActualHatchDate(request.getActualHatchDate());
        }

        IncubatorBatch updated = incubatorBatchRepository.save(batch);
        return hatchingMapper.toIncubatorResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncubatorResponse> searchIncubators(
            String batchCode,
            IncubatorStatus status,
            LocalDate expectedHatchDate,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Specification<IncubatorBatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchCode != null && !batchCode.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("batchCode")), "%" + batchCode.toLowerCase().trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (expectedHatchDate != null) {
                predicates.add(cb.equal(root.get("expectedHatchDate"), expectedHatchDate));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return incubatorBatchRepository.findAll(spec, pageable).map(hatchingMapper::toIncubatorResponse);
    }

    @Override
    @Transactional
    public HatchResultResponse saveHatchResult(HatchResultRequest request) {
        log.info("Saving hatch result for incubator ID: {}", request.getIncubatorBatchId());

        IncubatorBatch batch = incubatorBatchRepository.findById(request.getIncubatorBatchId())
                .orElseThrow(() -> new NotFoundException("Incubator batch not found with ID: " + request.getIncubatorBatchId()));

        int totalEggs = batch.getEggBatch() != null ? batch.getEggBatch().getTotalEggs() : 0;

        // Perform validations
        if (request.getFertileEggs() > totalEggs) {
            throw new ValidationException("Cannot record fertile eggs greater than total eggs.");
        }
        if (request.getHatchedChicks() > request.getFertileEggs()) {
            throw new ValidationException("Cannot hatch more chicks than fertile eggs.");
        }
        if (request.getDeadEmbryos() != null && request.getDeadEmbryos() > request.getFertileEggs()) {
            throw new ValidationException("Cannot record dead embryos greater than fertile eggs.");
        }

        int unhatchedEggs = Math.max(0, totalEggs - request.getHatchedChicks());

        double hatchPercentage = totalEggs > 0
                ? ((double) request.getHatchedChicks() / totalEggs) * 100.0
                : 0.0;

        HatchResult result = hatchingMapper.toHatchEntity(request);
        result.setIncubatorBatch(batch);
        result.setTotalEggs(totalEggs);
        result.setUnhatchedEggs(unhatchedEggs);
        result.setHatchPercentage(hatchPercentage);

        HatchResult savedHatchResult = hatchResultRepository.save(result);

        // Update incubator status to COMPLETED and set actual hatch date
        batch.setStatus(IncubatorStatus.COMPLETED);
        batch.setActualHatchDate(request.getRecordedDate());
        incubatorBatchRepository.save(batch);

        // Update underlying EggBatch status to HATCHED
        if (batch.getEggBatch() != null) {
            EggBatch eggBatch = batch.getEggBatch();
            eggBatch.setStatus(EggBatchStatus.HATCHED);
            eggBatch.setActualHatchDate(request.getRecordedDate());
            eggBatchRepository.save(eggBatch);
        }

        // Timeline event: Hatching Completed
        if (batch.getSourceHen() != null) {
            ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                    .chicken(batch.getSourceHen())
                    .title("Hatching Completed")
                    .description("Hatching completed for batch " + batch.getBatchCode() + ". Hatched: " + savedHatchResult.getHatchedChicks() + "/" + totalEggs + " (" + String.format("%.1f", hatchPercentage) + "%).")
                    .eventType("HATCHING_COMPLETED")
                    .createdBy("System")
                    .build();
            chickenTimelineRepository.save(event);
        }

        log.info("AUDIT: Hatch Completed. Incubator Batch Code: {}, Hatched Chicks: {}, Hatch %: {}",
                batch.getBatchCode(), savedHatchResult.getHatchedChicks(), savedHatchResult.getHatchPercentage());

        // Automatically generate Hatching Report
        try {
            hatchingReportService.generateHatchingReport(batch.getId());
            log.info("AUDIT: Automatic Hatching Report generated for batch: {}", batch.getBatchCode());
        } catch (Exception e) {
            log.error("Failed to auto-generate hatching report for batch: {}", batch.getBatchCode(), e);
        }

        return hatchingMapper.toHatchResponse(savedHatchResult);
    }

    @Override
    @Transactional(readOnly = true)
    public HatchResultResponse getHatchResultById(Long id) {
        log.info("Retrieving hatch result details for ID: {}", id);
        HatchResult result = hatchResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hatch result not found with ID: " + id));
        return hatchingMapper.toHatchResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HatchResultResponse> searchHatchResults(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching hatch results inside given time frame");

        Specification<HatchResult> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordedDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordedDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return hatchResultRepository.findAll(spec, pageable).map(hatchingMapper::toHatchResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrooderResponse> searchBrooders(
            String brooderCode,
            BrooderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching brooder cohorts with filters");

        Specification<BrooderBatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (brooderCode != null && !brooderCode.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("brooderCode")), "%" + brooderCode.toLowerCase().trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return brooderBatchRepository.findAll(spec, pageable).map(hatchingMapper::toBrooderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BrooderResponse getBrooderById(Long id) {
        log.info("Fetching brooder cohort details for ID: {}", id);
        BrooderBatch batch = brooderBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brooder batch not found with ID: " + id));
        return hatchingMapper.toBrooderResponse(batch);
    }

    @Override
    @Transactional
    public BrooderResponse completeBrooder(Long id) {
        log.info("Completing brooder cohort ID: {}", id);

        BrooderBatch batch = brooderBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brooder batch not found with ID: " + id));

        BrooderStatus oldStatus = batch.getStatus();
        batch.setStatus(BrooderStatus.COMPLETED);
        batch.setActualEndDate(LocalDate.now());

        BrooderBatch updated = brooderBatchRepository.save(batch);
        log.info("AUDIT: Brooder Status Change. ID: {}, status changed from {} to {}", id, oldStatus, updated.getStatus());

        if (batch.getHatchResult() != null) {
            List<Chicken> chicks = chickenRepository.findByHatchResultId(batch.getHatchResult().getId());
            for (Chicken chick : chicks) {
                if (chick.getStatus() == ChickenStatus.BROODER) {
                    chick.setStatus(ChickenStatus.GROWING);
                    chickenRepository.save(chick);
                    log.info("AUDIT: Chick status updated from BROODER to GROWING. Chicken ID: {}", chick.getId());
                }
            }
        }

        return hatchingMapper.toBrooderResponse(updated);
    }
}
