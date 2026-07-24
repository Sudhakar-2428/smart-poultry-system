package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.ChickGrowthMapper;
import com.poultry.backend.mapper.ChickenMapper;
import com.poultry.backend.repository.ChickGrowthRecordRepository;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.service.ChickGrowthService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChickGrowthServiceImpl implements ChickGrowthService {

    private final ChickGrowthRecordRepository growthRecordRepository;
    private final ChickenRepository chickenRepository;
    private final ChickGrowthMapper growthMapper;
    private final ChickenMapper chickenMapper;

    @Override
    @Transactional
    public ChickGrowthResponse createGrowthRecord(ChickGrowthRequest request) {
        log.info("Recording chick growth. Chicken ID: {}, Date: {}", request.getChickenId(), request.getGrowthDate());

        Chicken chicken = chickenRepository.findById(request.getChickenId())
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + request.getChickenId()));

        // Enforce validations
        if (chicken.getCategory() != ChickenCategory.CHICK) {
            throw new ValidationException("Only chickens with Category = CHICK may have growth records.");
        }
        if (chicken.getStatus() != ChickenStatus.BROODER && chicken.getStatus() != ChickenStatus.GROWING) {
            throw new ValidationException("Only chickens with Status = BROODER or GROWING may have growth records.");
        }
        if (request.getGrowthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Growth dates cannot be in the future.");
        }
        if (request.getWeight() <= 0) {
            throw new ValidationException("Weight must be greater than zero.");
        }
        if (growthRecordRepository.existsByChickenIdAndGrowthDate(request.getChickenId(), request.getGrowthDate())) {
            throw new DuplicateRecordException("Only one growth record is allowed per chick per day.");
        }

        int ageInDays = (int) ChronoUnit.DAYS.between(chicken.getDateOfBirth(), request.getGrowthDate());
        if (ageInDays < 0) {
            throw new ValidationException("Growth date cannot be before the chicken's date of birth.");
        }

        ChickGrowthRecord record = growthMapper.toEntity(request);
        record.setChicken(chicken);
        record.setAgeInDays(ageInDays);
        record.setGender(chicken.getGender());

        ChickGrowthRecord saved = growthRecordRepository.save(record);

        // Sync chicken current weight
        chicken.setWeight(request.getWeight());
        chickenRepository.save(chicken);

        log.info("AUDIT: Growth Record Created. ID: {}, Chicken Code: {}, Stage: {}",
                saved.getId(), chicken.getChickenCode(), saved.getGrowthStage());

        return growthMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ChickGrowthResponse getGrowthRecordById(Long id) {
        log.info("Retrieving growth record ID: {}", id);
        ChickGrowthRecord record = growthRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Growth record not found with ID: " + id));
        return growthMapper.toResponse(record);
    }

    @Override
    @Transactional
    public ChickGrowthResponse updateGrowthRecord(Long id, ChickGrowthRequest request) {
        log.info("Updating growth record ID: {}", id);

        ChickGrowthRecord record = growthRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Growth record not found with ID: " + id));

        Chicken chicken = record.getChicken();

        if (chicken.getStatus() == ChickenStatus.SOLD || chicken.getStatus() == ChickenStatus.DEAD) {
            throw new ValidationException("Cannot update growth records for a sold or deceased chicken.");
        }

        if (request.getGrowthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Growth dates cannot be in the future.");
        }
        if (request.getWeight() <= 0) {
            throw new ValidationException("Weight must be greater than zero.");
        }
        if (growthRecordRepository.existsByChickenIdAndGrowthDateAndIdNot(chicken.getId(), request.getGrowthDate(), id)) {
            throw new DuplicateRecordException("Only one growth record is allowed per chick per day.");
        }

        int ageInDays = (int) ChronoUnit.DAYS.between(chicken.getDateOfBirth(), request.getGrowthDate());
        if (ageInDays < 0) {
            throw new ValidationException("Growth date cannot be before the chicken's date of birth.");
        }

        record.setGrowthDate(request.getGrowthDate());
        record.setWeight(request.getWeight());
        record.setHeight(request.getHeight());
        record.setHealthStatus(request.getHealthStatus());
        record.setGrowthStage(request.getGrowthStage());
        record.setRemarks(request.getRemarks());
        record.setAgeInDays(ageInDays);

        ChickGrowthRecord updated = growthRecordRepository.save(record);

        // Sync chicken current weight
        chicken.setWeight(request.getWeight());
        chickenRepository.save(chicken);

        log.info("AUDIT: Growth Record Updated. ID: {}, Chicken Code: {}, Stage: {}",
                id, chicken.getChickenCode(), updated.getGrowthStage());

        return growthMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteGrowthRecord(Long id) {
        log.info("Deleting growth record ID: {}", id);
        ChickGrowthRecord record = growthRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Growth record not found with ID: " + id));

        growthRecordRepository.delete(record);
        log.info("AUDIT: Growth Record Deleted. ID: {}, Chicken Code: {}", id, record.getChicken().getChickenCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChickGrowthResponse> searchGrowthRecords(
            GrowthStage growthStage,
            Gender gender,
            HealthStatus healthStatus,
            Integer minAge,
            Integer maxAge,
            Double minWeight,
            Double maxWeight,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching growth records with filters");

        Specification<ChickGrowthRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (growthStage != null) {
                predicates.add(cb.equal(root.get("growthStage"), growthStage));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (healthStatus != null) {
                predicates.add(cb.equal(root.get("healthStatus"), healthStatus));
            }
            if (minAge != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ageInDays"), minAge));
            }
            if (maxAge != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("ageInDays"), maxAge));
            }
            if (minWeight != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weight"), minWeight));
            }
            if (maxWeight != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weight"), maxWeight));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("growthDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("growthDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return growthRecordRepository.findAll(spec, pageable).map(growthMapper::toResponse);
    }

    @Override
    @Transactional
    public ChickenResponse updateChickenGender(Long id, GenderUpdateRequest request) {
        log.info("Updating chicken gender for ID: {} to {}", id, request.getGender());

        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        // Business Rule: Gender cannot be changed after the bird reaches ADULT status unless performed by an ADMIN.
        if (chicken.getStatus() == ChickenStatus.ACTIVE || chicken.getCategory() != ChickenCategory.CHICK) {
            boolean isAdmin = false;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
            if (!isAdmin) {
                throw new ValidationException("Gender cannot be changed after the bird reaches ADULT status unless performed by an ADMIN.");
            }
        }

        Gender oldGender = chicken.getGender();
        chicken.setGender(request.getGender());
        Chicken saved = chickenRepository.save(chicken);

        log.info("AUDIT: Gender Identified. ID: {}, old gender: {}, new gender: {}",
                id, oldGender, request.getGender());

        return chickenMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ChickenResponse completeAdultTransition(Long id, AdultTransitionRequest request) {
        log.info("Completing adult transition for Chicken ID: {} to Category: {}", id, request.getCategory());

        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        if (chicken.getCategory() != ChickenCategory.CHICK) {
            throw new ValidationException("Chicken is not in CHICK category.");
        }

        ChickenCategory oldCategory = chicken.getCategory();
        ChickenStatus oldStatus = chicken.getStatus();

        // Perform category transformation
        if (request.getCategory() != ChickenCategory.BROILER &&
            request.getCategory() != ChickenCategory.LAYER &&
            request.getCategory() != ChickenCategory.BREEDER &&
            request.getCategory() != ChickenCategory.ROOSTER) {
            throw new ValidationException("Invalid transition category. Permitted: BROILER, LAYER, BREEDER, ROOSTER.");
        }

        chicken.setCategory(request.getCategory());
        chicken.setStatus(ChickenStatus.ACTIVE);
        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            chicken.setRemarks(chicken.getRemarks() == null ? request.getRemarks() : chicken.getRemarks() + " | " + request.getRemarks());
        }

        Chicken saved = chickenRepository.save(chicken);

        log.info("AUDIT: Adult Transition Completed. Chicken Code: {}, from [{} - {}] to [{} - {}]",
                chicken.getChickenCode(), oldCategory, oldStatus, saved.getCategory(), saved.getStatus());

        return chickenMapper.toResponse(saved);
    }
}
