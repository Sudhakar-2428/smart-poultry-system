package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.HealthRecordMapper;
import com.poultry.backend.repository.BreedingPairRepository;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.HealthRecordRepository;
import com.poultry.backend.repository.NotificationRepository;
import com.poultry.backend.service.HealthRecordService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository healthRepository;
    private final ChickenRepository chickenRepository;
    private final BreedingPairRepository breedingPairRepository;
    private final NotificationRepository notificationRepository;
    private final HealthRecordMapper healthMapper;

    @Override
    @Transactional
    public HealthRecordResponse createHealthRecord(HealthRecordRequest request) {
        log.info("Logging health record Code: {}, Type: {}", request.getRecordCode(), request.getHealthType());

        if (healthRepository.existsByRecordCode(request.getRecordCode())) {
            throw new DuplicateRecordException("Health record code '" + request.getRecordCode() + "' is already registered.");
        }

        Chicken chicken = chickenRepository.findById(request.getChickenId())
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + request.getChickenId()));

        if (chicken.getStatus() == ChickenStatus.DEAD) {
            throw new ValidationException("A deceased chicken cannot receive additional health records.");
        }
        if (chicken.getStatus() == ChickenStatus.SOLD) {
            throw new ValidationException("A sold chicken cannot receive additional health records.");
        }

        if (request.getRecordDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Record date cannot be in the future.");
        }

        if (request.getNextVaccinationDate() != null && request.getNextVaccinationDate().isBefore(request.getRecordDate())) {
            throw new ValidationException("Next vaccination date must be greater than or equal to the record date.");
        }

        // Vaccination Rule: Prevent duplicate vaccination records on the same day for the same vaccine.
        if (request.getHealthType() == HealthType.VACCINATION) {
            if (request.getVaccinationName() == null || request.getVaccinationName().trim().isEmpty()) {
                throw new ValidationException("Vaccination name is required for vaccination type records.");
            }
            if (healthRepository.existsByChickenIdAndRecordDateAndVaccinationNameAndHealthType(
                    chicken.getId(), request.getRecordDate(), request.getVaccinationName(), HealthType.VACCINATION)) {
                throw new DuplicateRecordException("Duplicate vaccination '" + request.getVaccinationName() + "' on the same day is prohibited.");
            }
        }

        // Treatment Validation
        if (request.getHealthType() == HealthType.TREATMENT) {
            if (request.getMedicineName() == null || request.getMedicineName().trim().isEmpty()) {
                throw new ValidationException("Medicine name is required for treatment records.");
            }
        }

        HealthRecord record = healthMapper.toEntity(request);
        record.setChicken(chicken);
        
        // If Health Status is deceased, force mortality = true
        if (request.getHealthStatus() == HealthStatus.DECEASED) {
            record.setMortality(true);
        }

        HealthRecord saved = healthRepository.save(record);

        // Process integrations for DECEASED state
        if (saved.getHealthStatus() == HealthStatus.DECEASED) {
            chicken.setStatus(ChickenStatus.DEAD);
            chicken.setPairId(null);
            chickenRepository.save(chicken);
            log.info("AUDIT: Mortality Recorded. Chicken code: {}", chicken.getChickenCode());

            // Remove from ACTIVE breeding pairs and clear pairId
            List<BreedingPair> activePairs = breedingPairRepository.findByChickenIdAndStatus(chicken.getId(), PairStatus.ACTIVE);
            for (BreedingPair pair : activePairs) {
                pair.setStatus(PairStatus.COMPLETED);
                pair.setEndDate(saved.getRecordDate());
                if (pair.getRemarks() == null) {
                    pair.setRemarks("Pair completed automatically due to demise of chicken " + chicken.getChickenCode());
                } else {
                    pair.setRemarks(pair.getRemarks() + " | Pair completed automatically due to demise of chicken " + chicken.getChickenCode());
                }
                breedingPairRepository.save(pair);

                // Clear pairId on the other chicken
                Chicken other = pair.getMaleChicken().getId().equals(chicken.getId())
                        ? pair.getFemaleChicken()
                        : pair.getMaleChicken();
                if (pair.getId().equals(other.getPairId())) {
                    other.setPairId(null);
                    chickenRepository.save(other);
                }
                log.info("AUDIT: Pair Completed. ID: {} due to death of chicken {}", pair.getId(), chicken.getChickenCode());
            }
        }

        // Generate notifications
        checkAndTriggerNotifications(saved);

        // Audit logs
        logAuditInfo(saved, "Created");

        return healthMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HealthRecordResponse getHealthRecordById(Long id) {
        log.info("Retrieving health record ID: {}", id);
        HealthRecord record = healthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Health record not found with ID: " + id));
        return healthMapper.toResponse(record);
    }

    @Override
    @Transactional
    public HealthRecordResponse updateHealthRecord(Long id, HealthRecordRequest request) {
        log.info("Updating health record ID: {}", id);

        HealthRecord record = healthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Health record not found with ID: " + id));

        Chicken chicken = record.getChicken();

        if (chicken.getStatus() == ChickenStatus.DEAD) {
            throw new ValidationException("A deceased chicken cannot receive additional health records.");
        }
        if (chicken.getStatus() == ChickenStatus.SOLD) {
            throw new ValidationException("A sold chicken cannot receive additional health records.");
        }

        if (healthRepository.existsByRecordCodeAndIdNot(request.getRecordCode(), id)) {
            throw new DuplicateRecordException("Health record code '" + request.getRecordCode() + "' is already registered.");
        }

        if (request.getRecordDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Record date cannot be in the future.");
        }

        if (request.getNextVaccinationDate() != null && request.getNextVaccinationDate().isBefore(request.getRecordDate())) {
            throw new ValidationException("Next vaccination date must be greater than or equal to the record date.");
        }

        if (request.getHealthType() == HealthType.VACCINATION) {
            if (request.getVaccinationName() == null || request.getVaccinationName().trim().isEmpty()) {
                throw new ValidationException("Vaccination name is required for vaccination type records.");
            }
            if (healthRepository.existsByChickenIdAndRecordDateAndVaccinationNameAndHealthTypeAndIdNot(
                    chicken.getId(), request.getRecordDate(), request.getVaccinationName(), HealthType.VACCINATION, id)) {
                throw new DuplicateRecordException("Duplicate vaccination '" + request.getVaccinationName() + "' on the same day is prohibited.");
            }
        }

        if (request.getHealthType() == HealthType.TREATMENT) {
            if (request.getMedicineName() == null || request.getMedicineName().trim().isEmpty()) {
                throw new ValidationException("Medicine name is required for treatment records.");
            }
        }

        record.setRecordCode(request.getRecordCode());
        record.setRecordDate(request.getRecordDate());
        record.setHealthType(request.getHealthType());
        record.setDiseaseName(request.getDiseaseName());
        record.setSymptoms(request.getSymptoms());
        record.setDiagnosis(request.getDiagnosis());
        record.setTreatment(request.getTreatment());
        record.setMedicineName(request.getMedicineName());
        record.setMedicineDose(request.getMedicineDose());
        record.setVaccinationName(request.getVaccinationName());
        record.setVaccinationBatch(request.getVaccinationBatch());
        record.setNextVaccinationDate(request.getNextVaccinationDate());
        record.setVeterinarian(request.getVeterinarian());
        record.setHealthStatus(request.getHealthStatus());
        record.setRemarks(request.getRemarks());

        if (request.getHealthStatus() == HealthStatus.DECEASED) {
            record.setMortality(true);
        } else if (request.getMortality() != null) {
            record.setMortality(request.getMortality());
        }

        HealthRecord saved = healthRepository.save(record);

        // If status changed to DECEASED during update
        if (saved.getHealthStatus() == HealthStatus.DECEASED && chicken.getStatus() != ChickenStatus.DEAD) {
            chicken.setStatus(ChickenStatus.DEAD);
            chicken.setPairId(null);
            chickenRepository.save(chicken);
            log.info("AUDIT: Mortality Recorded. Chicken code: {}", chicken.getChickenCode());

            // Remove from ACTIVE breeding pairs and clear pairId
            List<BreedingPair> activePairs = breedingPairRepository.findByChickenIdAndStatus(chicken.getId(), PairStatus.ACTIVE);
            for (BreedingPair pair : activePairs) {
                pair.setStatus(PairStatus.COMPLETED);
                pair.setEndDate(saved.getRecordDate());
                breedingPairRepository.save(pair);

                Chicken other = pair.getMaleChicken().getId().equals(chicken.getId())
                        ? pair.getFemaleChicken()
                        : pair.getMaleChicken();
                if (pair.getId().equals(other.getPairId())) {
                    other.setPairId(null);
                    chickenRepository.save(other);
                }
                log.info("AUDIT: Pair Completed. ID: {} due to death of chicken {}", pair.getId(), chicken.getChickenCode());
            }
        }

        checkAndTriggerNotifications(saved);
        logAuditInfo(saved, "Updated");

        return healthMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteHealthRecord(Long id) {
        log.info("Deleting health record ID: {}", id);
        HealthRecord record = healthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Health record not found with ID: " + id));

        healthRepository.delete(record);
        log.info("AUDIT: Health Record Deleted. Code: {}", record.getRecordCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HealthRecordSummaryResponse> searchHealthRecords(
            Long chickenId,
            HealthType healthType,
            HealthStatus healthStatus,
            String diseaseName,
            String vaccinationName,
            String veterinarian,
            LocalDate startDate,
            LocalDate endDate,
            Boolean mortality,
            Pageable pageable
    ) {
        log.info("Searching health records with filters");

        Specification<HealthRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (chickenId != null) {
                predicates.add(cb.equal(root.get("chicken").get("id"), chickenId));
            }
            if (healthType != null) {
                predicates.add(cb.equal(root.get("healthType"), healthType));
            }
            if (healthStatus != null) {
                predicates.add(cb.equal(root.get("healthStatus"), healthStatus));
            }
            if (diseaseName != null && !diseaseName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("diseaseName")), "%" + diseaseName.toLowerCase() + "%"));
            }
            if (vaccinationName != null && !vaccinationName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("vaccinationName")), "%" + vaccinationName.toLowerCase() + "%"));
            }
            if (veterinarian != null && !veterinarian.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("veterinarian")), "%" + veterinarian.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordDate"), endDate));
            }
            if (mortality != null) {
                predicates.add(cb.equal(root.get("mortality"), mortality));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return healthRepository.findAll(spec, pageable).map(healthMapper::toSummaryResponse);
    }

    // Reporting integration helpers
    @Override
    public long getTotalVaccinations() {
        return healthRepository.countByHealthType(HealthType.VACCINATION);
    }

    @Override
    public long getDiseaseCount() {
        return healthRepository.countByHealthType(HealthType.DISEASE);
    }

    @Override
    public long getMortalityCount() {
        return healthRepository.countByMortalityTrue();
    }

    @Override
    public long getRecoveryCount() {
        return healthRepository.countByHealthStatus(HealthStatus.RECOVERING);
    }

    @Override
    public long getTreatmentCount() {
        return healthRepository.countByHealthType(HealthType.TREATMENT);
    }

    @Override
    public double getVaccinationCompliance() {
        long total = getTotalVaccinations();
        if (total == 0) {
            return 100.0;
        }

        // Count how many vaccination records have future overdue nextVaccinationDates
        long overdue = healthRepository.findAll().stream()
                .filter(r -> r.getHealthType() == HealthType.VACCINATION && r.getNextVaccinationDate() != null)
                .filter(r -> r.getNextVaccinationDate().isBefore(LocalDate.now()))
                .count();

        double compliance = (double) (total - overdue) / total * 100.0;
        return Math.max(0.0, Math.min(100.0, compliance));
    }

    private void checkAndTriggerNotifications(HealthRecord record) {
        Chicken chicken = record.getChicken();
        
        if (record.getHealthType() == HealthType.VACCINATION && record.getNextVaccinationDate() != null) {
            LocalDate nextValDate = record.getNextVaccinationDate();
            long daysApart = ChronoUnit.DAYS.between(LocalDate.now(), nextValDate);
            
            if (daysApart < 0) {
                notificationRepository.save(Notification.builder()
                        .message("Vaccination campaign '" + record.getVaccinationName() + "' is overdue for chicken: " + chicken.getChickenCode())
                        .type("VACCINATION_OVERDUE")
                        .targetId(chicken.getId())
                        .build());
            } else if (daysApart <= 3) {
                notificationRepository.save(Notification.builder()
                        .message("Vaccination campaign '" + record.getVaccinationName() + "' is due on " + nextValDate + " for chicken: " + chicken.getChickenCode())
                        .type("VACCINATION_DUE")
                        .targetId(chicken.getId())
                        .build());
            }
        }

        if (record.getHealthStatus() == HealthStatus.CRITICAL) {
            notificationRepository.save(Notification.builder()
                    .message("CRITICAL disease condition triggered for: " + chicken.getChickenCode() + ". Disease: " + record.getDiseaseName())
                    .type("CRITICAL_DISEASE")
                    .targetId(chicken.getId())
                    .build());
        }

        if (record.getHealthStatus() == HealthStatus.DECEASED) {
            notificationRepository.save(Notification.builder()
                    .message("Breeder alert: Chicken " + chicken.getChickenCode() + " has died.")
                    .type("DECEASED")
                    .targetId(chicken.getId())
                    .build());
        }
    }

    private void logAuditInfo(HealthRecord record, String mutation) {
        if (record.getHealthType() == HealthType.VACCINATION && mutation.equals("Created")) {
            log.info("AUDIT: Vaccination Created. Code: {}, Vaccine: {}", record.getRecordCode(), record.getVaccinationName());
        } else if (record.getHealthType() == HealthType.TREATMENT && mutation.equals("Created")) {
            log.info("AUDIT: Treatment Recorded. Code: {}, Medicine: {}", record.getRecordCode(), record.getMedicineName());
        } else if (record.getHealthType() == HealthType.DISEASE && mutation.equals("Created")) {
            log.info("AUDIT: Disease Recorded. Code: {}, Disease: {}", record.getRecordCode(), record.getDiseaseName());
        }

        if (record.getHealthStatus() == HealthStatus.DECEASED && mutation.equals("Created")) {
            log.info("AUDIT: Mortality Recorded. Code: {}, Chicken: {}", record.getRecordCode(), record.getChicken().getChickenCode());
        }

        log.info("AUDIT: Health Record {}. ID: {}, status: {}", mutation, record.getId(), record.getHealthStatus());
    }
}
