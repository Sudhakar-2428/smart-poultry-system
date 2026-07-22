package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.EggMapper;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.EggBatchRepository;
import com.poultry.backend.repository.EggRecordRepository;
import com.poultry.backend.repository.FarmSettingRepository;
import com.poultry.backend.service.EggService;
import jakarta.persistence.criteria.Join;
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
public class EggServiceImpl implements EggService {

    private final EggRecordRepository eggRecordRepository;
    private final EggBatchRepository eggBatchRepository;
    private final ChickenRepository chickenRepository;
    private final FarmSettingRepository farmSettingRepository;
    private final EggMapper eggMapper;

    @Override
    @Transactional
    public EggRecordResponse recordDailyEggs(EggRecordRequest request) {
        log.info("Recording daily eggs for hen ID: {}", request.getHenId());

        // Validate damaged eggs count
        if (request.getDamagedEggs() > request.getNumberOfEggs()) {
            throw new ValidationException("Damaged eggs cannot exceed total eggs.");
        }

        // Validate record date is not in future
        if (request.getRecordDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Record date cannot be in the future.");
        }

        Chicken hen = chickenRepository.findById(request.getHenId())
                .orElseThrow(() -> new NotFoundException("Hen not found with ID: " + request.getHenId()));

        // Validate biological rules
        if (hen.getGender() != Gender.FEMALE) {
            throw new ValidationException("Only female chickens can lay eggs. Roosters cannot lay eggs.");
        }
        if (hen.getStatus() == ChickenStatus.DEAD || hen.getStatus() == ChickenStatus.SOLD) {
            throw new ValidationException("Dead or Sold chickens cannot record eggs.");
        }
        if (hen.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE female chickens can record eggs.");
        }

        EggRecord record = eggMapper.toRecordEntity(request);
        record.setHen(hen);
        EggRecord savedRecord = eggRecordRepository.save(record);

        log.info("AUDIT: Daily egg recording registered. ID: {}, Hen ID: {}, Count: {}, Damaged: {}",
                savedRecord.getId(), hen.getId(), savedRecord.getNumberOfEggs(), savedRecord.getDamagedEggs());

        return eggMapper.toRecordResponse(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public EggRecordResponse getEggRecordById(Long id) {
        log.info("Retrieving details of egg record ID: {}", id);
        EggRecord record = eggRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg record not found with ID: " + id));
        return eggMapper.toRecordResponse(record);
    }

    @Override
    @Transactional
    public EggRecordResponse updateEggRecord(Long id, EggRecordRequest request) {
        log.info("Updating egg record ID: {}", id);

        EggRecord record = eggRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg record not found with ID: " + id));

        if (request.getDamagedEggs() > request.getNumberOfEggs()) {
            throw new ValidationException("Damaged eggs cannot exceed total eggs.");
        }

        if (request.getRecordDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Record date cannot be in the future.");
        }

        // Check if hen changed
        if (!record.getHen().getId().equals(request.getHenId())) {
            Chicken newHen = chickenRepository.findById(request.getHenId())
                    .orElseThrow(() -> new NotFoundException("Hen not found with ID: " + request.getHenId()));

            if (newHen.getGender() != Gender.FEMALE) {
                throw new ValidationException("Only female chickens can lay eggs. Roosters cannot lay eggs.");
            }
            if (newHen.getStatus() == ChickenStatus.DEAD || newHen.getStatus() == ChickenStatus.SOLD) {
                throw new ValidationException("Dead or Sold chickens cannot record eggs.");
            }
            if (newHen.getStatus() != ChickenStatus.ACTIVE) {
                throw new ValidationException("Only ACTIVE female chickens can record eggs.");
            }
            record.setHen(newHen);
        }

        record.setRecordDate(request.getRecordDate());
        record.setNumberOfEggs(request.getNumberOfEggs());
        record.setDamagedEggs(request.getDamagedEggs());
        record.setRemarks(request.getRemarks());

        EggRecord updatedRecord = eggRecordRepository.save(record);
        log.info("AUDIT: Egg record updated. ID: {}, Hen ID: {}", id, updatedRecord.getHen().getId());

        return eggMapper.toRecordResponse(updatedRecord);
    }

    @Override
    @Transactional
    public void deleteEggRecord(Long id) {
        log.info("Deleting egg record ID: {}", id);
        EggRecord record = eggRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg record not found with ID: " + id));

        eggRecordRepository.delete(record);
        log.info("AUDIT: Egg record deleted. ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggRecordResponse> searchEggRecords(
            Long henId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching egg records. Hen ID: {}, Date Range: [{} to {}]", henId, startDate, endDate);

        Specification<EggRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (henId != null) {
                predicates.add(cb.equal(root.get("hen").get("id"), henId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eggRecordRepository.findAll(spec, pageable).map(eggMapper::toRecordResponse);
    }

    @Override
    @Transactional
    public EggBatchResponse createEggBatch(EggBatchRequest request) {
        log.info("Creating new egg batch. Code: {}", request.getBatchCode());

        if (eggBatchRepository.existsByBatchCode(request.getBatchCode())) {
            throw new DuplicateRecordException("Egg batch code '" + request.getBatchCode() + "' is already registered.");
        }

        if (request.getDamagedEggs() > request.getTotalEggs()) {
            throw new ValidationException("Damaged eggs cannot exceed total eggs.");
        }

        Chicken sourceHen = null;
        if (request.getSourceHenId() != null) {
            sourceHen = chickenRepository.findById(request.getSourceHenId())
                    .orElseThrow(() -> new NotFoundException("Source hen not found with ID: " + request.getSourceHenId()));

            // Validate biological rules for lay origin
            if (sourceHen.getGender() != Gender.FEMALE) {
                throw new ValidationException("Only female chickens can record eggs. Roosters cannot lay eggs.");
            }
            if (sourceHen.getStatus() == ChickenStatus.DEAD || sourceHen.getStatus() == ChickenStatus.SOLD) {
                throw new ValidationException("Dead or Sold chickens cannot record eggs.");
            }
            if (sourceHen.getStatus() != ChickenStatus.ACTIVE) {
                throw new ValidationException("Only ACTIVE female chickens can record eggs.");
            }
        }

        EggBatch batch = eggMapper.toBatchEntity(request);
        batch.setSourceHen(sourceHen);

        // Read incubation days from settings DB or default to 21
        int incubationDays = 21;
        try {
            incubationDays = farmSettingRepository.findById("INCUBATION_DAYS")
                    .map(setting -> Integer.parseInt(setting.getValue()))
                    .orElse(21);
        } catch (Exception e) {
            log.warn("Failed lookup of INCUBATION_DAYS, defaulting to 21", e);
        }

        LocalDate expectedHatchDate = request.getBatchDate().plusDays(incubationDays);
        batch.setExpectedHatchDate(expectedHatchDate);

        EggBatch savedBatch = eggBatchRepository.save(batch);

        log.info("AUDIT: Egg batch created. Code: {}, Total: {}, Expected Hatch: {}",
                savedBatch.getBatchCode(), savedBatch.getTotalEggs(), savedBatch.getExpectedHatchDate());

        return eggMapper.toBatchResponse(savedBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public EggBatchResponse getEggBatchById(Long id) {
        log.info("Retrieving detailed egg batch ID: {}", id);
        EggBatch batch = eggBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + id));
        return eggMapper.toBatchResponse(batch);
    }

    @Override
    @Transactional
    public EggBatchResponse updateEggBatch(Long id, EggBatchRequest request) {
        log.info("Updating egg batch ID: {}", id);

        EggBatch batch = eggBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + id));

        // Code uniqueness check
        if (!batch.getBatchCode().equalsIgnoreCase(request.getBatchCode())) {
            if (eggBatchRepository.existsByBatchCode(request.getBatchCode())) {
                throw new DuplicateRecordException("Egg batch code '" + request.getBatchCode() + "' is already registered.");
            }
        }

        if (request.getDamagedEggs() > request.getTotalEggs()) {
            throw new ValidationException("Damaged eggs cannot exceed total eggs.");
        }

        Chicken sourceHen = null;
        if (request.getSourceHenId() != null) {
            sourceHen = chickenRepository.findById(request.getSourceHenId())
                    .orElseThrow(() -> new NotFoundException("Source hen not found with ID: " + request.getSourceHenId()));

            if (sourceHen.getGender() != Gender.FEMALE) {
                throw new ValidationException("Only female chickens can record eggs. Roosters cannot lay eggs.");
            }
            if (sourceHen.getStatus() == ChickenStatus.DEAD || sourceHen.getStatus() == ChickenStatus.SOLD) {
                throw new ValidationException("Dead or Sold chickens cannot record eggs.");
            }
            if (sourceHen.getStatus() != ChickenStatus.ACTIVE) {
                throw new ValidationException("Only ACTIVE female chickens can record eggs.");
            }
        }

        eggMapper.updateBatchEntityFromRequest(request, batch);
        batch.setSourceHen(sourceHen);

        int incubationDays = 21;
        try {
            incubationDays = farmSettingRepository.findById("INCUBATION_DAYS")
                    .map(setting -> Integer.parseInt(setting.getValue()))
                    .orElse(21);
        } catch (Exception e) {
            log.warn("Failed lookup of INCUBATION_DAYS, defaulting to 21", e);
        }
        batch.setExpectedHatchDate(request.getBatchDate().plusDays(incubationDays));

        EggBatch updatedBatch = eggBatchRepository.save(batch);
        log.info("AUDIT: Egg batch updated. ID: {}, Code: {}", id, updatedBatch.getBatchCode());

        return eggMapper.toBatchResponse(updatedBatch);
    }

    @Override
    @Transactional
    public EggBatchResponse changeBatchStatus(Long id, EggBatchStatusRequest request) {
        log.info("Changing status of egg batch ID: {} to {}", id, request.getStatus());

        EggBatch batch = eggBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + id));

        EggBatchStatus oldStatus = batch.getStatus();
        EggBatchStatus newStatus = request.getStatus();

        batch.setStatus(newStatus);
        if (request.getActualHatchDate() != null) {
            batch.setActualHatchDate(request.getActualHatchDate());
        }

        EggBatch updatedBatch = eggBatchRepository.save(batch);

        log.info("AUDIT: Egg batch status changed for ID: {} from {} to {}", id, oldStatus, newStatus);

        return eggMapper.toBatchResponse(updatedBatch);
    }

    @Override
    @Transactional
    public void deleteEggBatch(Long id) {
        log.info("Deleting egg batch ID: {}", id);
        EggBatch batch = eggBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egg batch not found with ID: " + id));

        String code = batch.getBatchCode();
        eggBatchRepository.delete(batch);

        log.info("AUDIT: Egg batch deleted. ID: {}, Code: {}", id, code);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EggBatchSummaryResponse> searchEggBatches(
            String batchCode,
            EggBatchStatus status,
            EggPurpose purpose,
            LocalDate expectedHatchDate,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching egg batches. Sort: {}", pageable.getSort());

        Specification<EggBatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchCode != null && !batchCode.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("batchCode")), "%" + batchCode.toLowerCase().trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (purpose != null) {
                predicates.add(cb.equal(root.get("purpose"), purpose));
            }
            if (expectedHatchDate != null) {
                predicates.add(cb.equal(root.get("expectedHatchDate"), expectedHatchDate));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("batchDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("batchDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eggBatchRepository.findAll(spec, pageable).map(eggMapper::toBatchSummaryResponse);
    }
}
