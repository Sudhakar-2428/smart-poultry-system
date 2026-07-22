package com.poultry.backend.service.impl;

import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.Chicken;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.ChickenMapper;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.service.ChickenService;
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
public class ChickenServiceImpl implements ChickenService {

    private final ChickenRepository chickenRepository;
    private final ChickenMapper chickenMapper;

    @Override
    @Transactional
    public ChickenResponse createChicken(ChickenRequest request) {
        log.info("Processing instruction to register new chicken. Code: {}", request.getChickenCode());

        // Validate chickenCode uniqueness
        if (chickenRepository.existsByChickenCode(request.getChickenCode())) {
            throw new DuplicateRecordException("Chicken code '" + request.getChickenCode() + "' is already registered.");
        }

        // Validate DOB cannot be in the future
        if (request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        Chicken chicken = chickenMapper.toEntity(request);
        Chicken savedChicken = chickenRepository.save(chicken);

        log.info("AUDIT: Chicken registration processed. Code: {}, Breed: {}, Category: {}, Status: {}",
                savedChicken.getChickenCode(), savedChicken.getBreed(), savedChicken.getCategory(), savedChicken.getStatus());

        return chickenMapper.toResponse(savedChicken);
    }

    @Override
    @Transactional(readOnly = true)
    public ChickenResponse getChickenById(Long id) {
        log.info("Retrieving details of chicken for ID: {}", id);
        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));
        return chickenMapper.toResponse(chicken);
    }

    @Override
    @Transactional
    public ChickenResponse updateChicken(Long id, ChickenRequest request) {
        log.info("Processing instruction to update details of chicken. ID: {}, Code: {}", id, request.getChickenCode());

        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        // Validate chickenCode uniqueness if changed
        if (!chicken.getChickenCode().equalsIgnoreCase(request.getChickenCode())) {
            if (chickenRepository.existsByChickenCode(request.getChickenCode())) {
                throw new DuplicateRecordException("Chicken code '" + request.getChickenCode() + "' is already registered.");
            }
        }

        // Validate DOB cannot be in the future
        if (request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        // Auditing status changes
        ChickenStatus oldStatus = chicken.getStatus();
        ChickenStatus newStatus = request.getStatus();

        chickenMapper.updateEntityFromRequest(request, chicken);
        Chicken updatedChicken = chickenRepository.save(chicken);

        log.info("AUDIT: Chicken update processed. ID: {}, Code: {}", id, updatedChicken.getChickenCode());

        if (oldStatus != newStatus) {
            log.info("AUDIT: Chicken status changed for ID: {} from {} to {}", id, oldStatus, newStatus);
        }

        return chickenMapper.toResponse(updatedChicken);
    }

    @Override
    @Transactional
    public void deleteChicken(Long id) {
        log.info("Processing instruction to delete chicken. ID: {}", id);

        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        // Validate business rule: Cannot delete SOLD or DEAD chickens
        if (chicken.getStatus() == ChickenStatus.SOLD) {
            throw new ValidationException("Cannot delete SOLD chickens.");
        }
        if (chicken.getStatus() == ChickenStatus.DEAD) {
            throw new ValidationException("Cannot delete DEAD chickens.");
        }

        String code = chicken.getChickenCode();
        chickenRepository.delete(chicken);

        log.info("AUDIT: Chicken deletion processed. ID: {}, Code: {}", id, code);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChickenSummaryResponse> searchChickens(
            Breed breed,
            Gender gender,
            ChickenCategory category,
            ChickenStatus status,
            Integer minAgeDays,
            Integer maxAgeDays,
            Double minWeight,
            Double maxWeight,
            String chickenCode,
            String name,
            Pageable pageable
    ) {
        log.info("Searching chickens with dynamic filters. Sort: {}", pageable.getSort());

        Specification<Chicken> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (breed != null) {
                predicates.add(cb.equal(root.get("breed"), breed));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Search by Chicken Code (partial, case-insensitive)
            if (chickenCode != null && !chickenCode.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("chickenCode")), "%" + chickenCode.toLowerCase().trim() + "%"));
            }

            // Search by Name (partial, case-insensitive)
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%"));
            }

            // Weight Range
            if (minWeight != null) {
                predicates.add(cb.ge(root.get("weight"), minWeight));
            }
            if (maxWeight != null) {
                predicates.add(cb.le(root.get("weight"), maxWeight));
            }

            // Calculated Age dynamic range mappings over dateOfBirth column
            // DOB earliest: latest (youngest) fits max age; oldest fits min age.
            if (minAgeDays != null) {
                LocalDate oldestDob = LocalDate.now().minusDays(minAgeDays);
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), oldestDob));
            }
            if (maxAgeDays != null) {
                LocalDate youngestDob = LocalDate.now().minusDays(maxAgeDays);
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfBirth"), youngestDob));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return chickenRepository.findAll(spec, pageable).map(chickenMapper::toSummaryResponse);
    }
}
