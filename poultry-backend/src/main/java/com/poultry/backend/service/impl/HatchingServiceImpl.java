package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.HatchingMapper;
import com.poultry.backend.repository.*;
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
    private final HatchingMapper hatchingMapper;

    @Override
    @Transactional
    public IncubatorResponse createIncubator(IncubatorRequest request) {
        log.info("Creating incubator batch. Code: {}, Egg Batch ID: {}", request.getBatchCode(), request.getEggBatchId());

        if (incubatorBatchRepository.existsByBatchCode(request.getBatchCode())) {
            throw new DuplicateRecordException("Incubator batch code '" + request.getBatchCode() + "' is already registered.");
        }

        EggBatch eggBatch = eggBatchRepository.findById(request.getEggBatchId())
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + request.getEggBatchId()));

        // Enforce business rules
        if (eggBatch.getPurpose() != EggPurpose.HATCHING) {
            throw new ValidationException("Only Egg Batches with purpose = HATCHING may enter incubation.");
        }
        if (eggBatch.getStatus() != EggBatchStatus.CREATED && eggBatch.getStatus() != EggBatchStatus.BROODING) {
            throw new ValidationException("Only CREATED or BROODING batches may start incubation.");
        }

        IncubatorBatch incubatorBatch = hatchingMapper.toIncubatorEntity(request);
        incubatorBatch.setEggBatch(eggBatch);
        // Expected Hatch Date is copied from Egg Batch
        incubatorBatch.setExpectedHatchDate(eggBatch.getExpectedHatchDate());

        IncubatorBatch savedBatch = incubatorBatchRepository.save(incubatorBatch);

        // Update Egg Batch status to INCUBATING
        eggBatch.setStatus(EggBatchStatus.INCUBATING);
        eggBatchRepository.save(eggBatch);

        log.info("AUDIT: Incubation Started. Incubator Batch Code: {}, Egg Batch ID: {}",
                savedBatch.getBatchCode(), eggBatch.getId());

        return hatchingMapper.toIncubatorResponse(savedBatch);
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

        if (eggBatch.getPurpose() != EggPurpose.HATCHING) {
            throw new ValidationException("Only Egg Batches with purpose = HATCHING may enter incubation.");
        }

        batch.setBatchCode(request.getBatchCode());
        batch.setEggBatch(eggBatch);
        batch.setStartDate(request.getStartDate());
        batch.setExpectedHatchDate(eggBatch.getExpectedHatchDate());
        batch.setStatus(request.getStatus());
        batch.setTemperature(request.getTemperature());
        batch.setHumidity(request.getHumidity());
        batch.setNotes(request.getNotes());

        IncubatorBatch updated = incubatorBatchRepository.save(batch);
        log.info("AUDIT: Incubator Status Change. ID: {}, status: {}", id, updated.getStatus());

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
        log.info("AUDIT: Status Changes. Incubator ID: {}, status: {}", id, request.getStatus());

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
        log.info("Searching incubator batches with filters");

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
        if (request.getDeadEmbryos() > request.getFertileEggs()) {
            throw new ValidationException("Cannot record dead embryos greater than fertile eggs.");
        }

        // Calculate unhatched eggs
        int unhatchedEggs = Math.max(0, totalEggs - request.getHatchedChicks());

        // Calculate Hatch Percentage
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

        log.info("AUDIT: Hatch Completed. Incubator Batch Code: {}, Hatched Chicks: {}, Hatch %: {}",
                batch.getBatchCode(), savedHatchResult.getHatchedChicks(), savedHatchResult.getHatchPercentage());

        // 7. Automatic Chick Registration
        Breed breed = Breed.OTHER;
        if (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null) {
            breed = batch.getEggBatch().getSourceHen().getBreed();
        }

        Long motherId = (batch.getEggBatch() != null && batch.getEggBatch().getSourceHen() != null)
                ? batch.getEggBatch().getSourceHen().getId()
                : null;

        Long eggBatchId = batch.getEggBatch() != null ? batch.getEggBatch().getId() : null;

        for (int i = 1; i <= request.getHatchedChicks(); i++) {
            // Generate clean unique chicken code using result id & index & random snippet for safety
            String code = "CK-" + savedHatchResult.getId() + "-" + String.format("%04d", i);
            Chicken chick = Chicken.builder()
                    .chickenCode(code)
                    .breed(breed)
                    .category(ChickenCategory.CHICK)
                    .gender(Gender.UNKNOWN)
                    .dateOfBirth(request.getRecordedDate())
                    .status(ChickenStatus.BROODER)
                    .hatchResultId(savedHatchResult.getId())
                    .eggBatchId(eggBatchId)
                    .motherId(motherId)
                    .build();
            chickenRepository.save(chick);
        }
        log.info("AUDIT: Automatic Chick Registration completed. Hatch Result ID: {}, Count: {}",
                savedHatchResult.getId(), request.getHatchedChicks());

        // 8. Automatic Brooder Creation
        int brooderPeriod = 10;
        try {
            brooderPeriod = farmSettingRepository.findById("BROODER_PERIOD")
                    .map(setting -> Integer.parseInt(setting.getValue()))
                    .orElse(10);
        } catch (Exception e) {
            log.warn("Failed lookup of BROODER_PERIOD setting, defaulting to 10", e);
        }

        LocalDate expectedEndDate = request.getRecordedDate().plusDays(brooderPeriod);
        String brooderCode = "BRD-" + batch.getBatchCode();
        if (brooderBatchRepository.existsByBrooderCode(brooderCode)) {
            brooderCode += "-" + System.currentTimeMillis() % 1000;
        }

        BrooderBatch brooder = BrooderBatch.builder()
                .brooderCode(brooderCode)
                .hatchResult(savedHatchResult)
                .startDate(request.getRecordedDate())
                .expectedEndDate(expectedEndDate)
                .status(BrooderStatus.ACTIVE)
                .remarks("Automatically created from Hatch Result of Incubator Batch " + batch.getBatchCode())
                .build();

        BrooderBatch savedBrooder = brooderBatchRepository.save(brooder);
        log.info("AUDIT: Brooder Created. Brooder Code: {}, Hatch Result ID: {}",
                savedBrooder.getBrooderCode(), savedHatchResult.getId());

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
