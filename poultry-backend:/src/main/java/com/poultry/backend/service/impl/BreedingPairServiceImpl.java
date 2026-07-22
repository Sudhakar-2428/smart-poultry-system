package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.BreedingPairMapper;
import com.poultry.backend.repository.BreedingPairRepository;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.service.BreedingPairService;
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
public class BreedingPairServiceImpl implements BreedingPairService {

    private final BreedingPairRepository breedingPairRepository;
    private final ChickenRepository chickenRepository;
    private final BreedingPairMapper pairMapper;

    @Override
    @Transactional
    public BreedingPairResponse createPair(BreedingPairRequest request) {
        log.info("Creating breeding pair. Code: {}", request.getPairCode());

        if (breedingPairRepository.existsByPairCode(request.getPairCode())) {
            throw new DuplicateRecordException("Breeding pair code '" + request.getPairCode() + "' is already registered.");
        }

        if (request.getMaleChickenId().equals(request.getFemaleChickenId())) {
            throw new ValidationException("Male and female chickens cannot be the same chicken.");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }

        Chicken male = chickenRepository.findById(request.getMaleChickenId())
                .orElseThrow(() -> new NotFoundException("Male chicken not found with ID: " + request.getMaleChickenId()));

        Chicken female = chickenRepository.findById(request.getFemaleChickenId())
                .orElseThrow(() -> new NotFoundException("Female chicken not found with ID: " + request.getFemaleChickenId()));

        // Bio-validations
        if (male.getGender() != Gender.MALE) {
            throw new ValidationException("Male chicken gender must be MALE.");
        }
        if (male.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Male chicken status must be ACTIVE.");
        }
        if (male.getCategory() != ChickenCategory.ROOSTER && male.getCategory() != ChickenCategory.BREEDER) {
            throw new ValidationException("Male chicken category must be ROOSTER or BREEDER.");
        }

        if (female.getGender() != Gender.FEMALE) {
            throw new ValidationException("Female chicken gender must be FEMALE.");
        }
        if (female.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Female chicken status must be ACTIVE.");
        }
        if (female.getCategory() != ChickenCategory.LAYER && female.getCategory() != ChickenCategory.BREEDER) {
            throw new ValidationException("Female chicken category must be LAYER or BREEDER.");
        }

        // Uniqueness of active pair: "A chicken cannot belong to multiple ACTIVE breeding pairs."
        if (request.getStatus() == PairStatus.ACTIVE) {
            if (breedingPairRepository.existsByMaleChickenIdAndStatus(male.getId(), PairStatus.ACTIVE)) {
                throw new ValidationException("Male chicken is already assigned to an active pair.");
            }
            if (breedingPairRepository.existsByFemaleChickenIdAndStatus(female.getId(), PairStatus.ACTIVE)) {
                throw new ValidationException("Female chicken is already assigned to an active pair.");
            }
        }

        BreedingPair pair = pairMapper.toEntity(request);
        pair.setMaleChicken(male);
        pair.setFemaleChicken(female);

        BreedingPair saved = breedingPairRepository.save(pair);

        // Sync pairId on both chickens
        syncChickenPairIds(saved);

        log.info("AUDIT: Pair Created. Code: {}, status: {}", saved.getPairCode(), saved.getStatus());
        if (saved.getStatus() == PairStatus.ACTIVE) {
            log.info("AUDIT: Pair Activated. ID: {}", saved.getId());
        }

        return pairMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BreedingPairResponse getPairById(Long id) {
        log.info("Retrieving breeding pair ID: {}", id);
        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));
        return pairMapper.toResponse(pair);
    }

    @Override
    @Transactional
    public BreedingPairResponse updatePair(Long id, BreedingPairRequest request) {
        log.info("Updating breeding pair ID: {}", id);

        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        if (breedingPairRepository.existsByPairCodeAndIdNot(request.getPairCode(), id)) {
            throw new DuplicateRecordException("Breeding pair code '" + request.getPairCode() + "' is already registered.");
        }

        if (request.getMaleChickenId().equals(request.getFemaleChickenId())) {
            throw new ValidationException("Male and female chickens cannot be the same chicken.");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }

        Chicken male = chickenRepository.findById(request.getMaleChickenId())
                .orElseThrow(() -> new NotFoundException("Male chicken not found with ID: " + request.getMaleChickenId()));

        Chicken female = chickenRepository.findById(request.getFemaleChickenId())
                .orElseThrow(() -> new NotFoundException("Female chicken not found with ID: " + request.getFemaleChickenId()));

        // Bio-validations
        if (male.getGender() != Gender.MALE) {
            throw new ValidationException("Male chicken gender must be MALE.");
        }
        if (male.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Male chicken status must be ACTIVE.");
        }
        if (male.getCategory() != ChickenCategory.ROOSTER && male.getCategory() != ChickenCategory.BREEDER) {
            throw new ValidationException("Male chicken category must be ROOSTER or BREEDER.");
        }

        if (female.getGender() != Gender.FEMALE) {
            throw new ValidationException("Female chicken gender must be FEMALE.");
        }
        if (female.getStatus() != ChickenStatus.ACTIVE) {
            throw new ValidationException("Female chicken status must be ACTIVE.");
        }
        if (female.getCategory() != ChickenCategory.LAYER && female.getCategory() != ChickenCategory.BREEDER) {
            throw new ValidationException("Female chicken category must be LAYER or BREEDER.");
        }

        // Active pair validations
        if (request.getStatus() == PairStatus.ACTIVE) {
            if (breedingPairRepository.existsByMaleChickenIdAndStatusAndIdNot(male.getId(), PairStatus.ACTIVE, id)) {
                throw new ValidationException("Male chicken is already assigned to an active pair.");
            }
            if (breedingPairRepository.existsByFemaleChickenIdAndStatusAndIdNot(female.getId(), PairStatus.ACTIVE, id)) {
                throw new ValidationException("Female chicken is already assigned to an active pair.");
            }
        }

        PairStatus oldStatus = pair.getStatus();

        // Save original chickens for clearing pairId if they were modified
        Chicken originalMale = pair.getMaleChicken();
        Chicken originalFemale = pair.getFemaleChicken();

        pair.setPairCode(request.getPairCode());
        pair.setMaleChicken(male);
        pair.setFemaleChicken(female);
        pair.setStartDate(request.getStartDate());
        pair.setEndDate(request.getEndDate());
        pair.setStatus(request.getStatus());
        pair.setBreedingPurpose(request.getBreedingPurpose());
        pair.setExpectedEggProduction(request.getExpectedEggProduction());
        pair.setRemarks(request.getRemarks());

        BreedingPair updated = breedingPairRepository.save(pair);

        // If chickens changed, clear pairId from original ones
        if (!originalMale.getId().equals(male.getId())) {
            if (updated.getId().equals(originalMale.getPairId())) {
                originalMale.setPairId(null);
                chickenRepository.save(originalMale);
            }
        }
        if (!originalFemale.getId().equals(female.getId())) {
            if (updated.getId().equals(originalFemale.getPairId())) {
                originalFemale.setPairId(null);
                chickenRepository.save(originalFemale);
            }
        }

        // Synchronize pairId on active/updated chickens
        syncChickenPairIds(updated);

        log.info("AUDIT: Pair Updated. ID: {}, status: {}", id, updated.getStatus());
        if (oldStatus != PairStatus.ACTIVE && updated.getStatus() == PairStatus.ACTIVE) {
            log.info("AUDIT: Pair Activated. ID: {}", id);
        } else if (oldStatus == PairStatus.ACTIVE && updated.getStatus() == PairStatus.COMPLETED) {
            log.info("AUDIT: Pair Completed. ID: {}", id);
        } else if (oldStatus == PairStatus.ACTIVE && updated.getStatus() == PairStatus.CANCELLED) {
            log.info("AUDIT: Pair Cancelled. ID: {}", id);
        }

        return pairMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public BreedingPairResponse updatePairStatus(Long id, PairStatusUpdateRequest request) {
        log.info("Patching breeding pair status for ID: {} to {}", id, request.getStatus());

        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        PairStatus oldStatus = pair.getStatus();

        if (request.getStatus() == PairStatus.ACTIVE) {
            if (breedingPairRepository.existsByMaleChickenIdAndStatusAndIdNot(pair.getMaleChicken().getId(), PairStatus.ACTIVE, id)) {
                throw new ValidationException("Male chicken is already assigned to an active pair.");
            }
            if (breedingPairRepository.existsByFemaleChickenIdAndStatusAndIdNot(pair.getFemaleChicken().getId(), PairStatus.ACTIVE, id)) {
                throw new ValidationException("Female chicken is already assigned to an active pair.");
            }
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(pair.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }

        pair.setStatus(request.getStatus());
        if (request.getEndDate() != null) {
            pair.setEndDate(request.getEndDate());
        } else if (request.getStatus() == PairStatus.COMPLETED || request.getStatus() == PairStatus.CANCELLED) {
            // Automatically capture today if not provided on complete/cancel
            pair.setEndDate(LocalDate.now());
        }

        BreedingPair saved = breedingPairRepository.save(pair);

        // Sync pairId on associated chickens
        syncChickenPairIds(saved);

        if (oldStatus != PairStatus.ACTIVE && saved.getStatus() == PairStatus.ACTIVE) {
            log.info("AUDIT: Pair Activated. ID: {}", id);
        } else if (saved.getStatus() == PairStatus.COMPLETED) {
            log.info("AUDIT: Pair Completed. ID: {}", id);
        } else if (saved.getStatus() == PairStatus.CANCELLED) {
            log.info("AUDIT: Pair Cancelled. ID: {}", id);
        }

        return pairMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePair(Long id) {
        log.info("Deleting breeding pair ID: {}", id);
        BreedingPair pair = breedingPairRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Breeding pair not found with ID: " + id));

        // Clear pairId on chickens before delete
        Chicken male = pair.getMaleChicken();
        Chicken female = pair.getFemaleChicken();
        if (id.equals(male.getPairId())) {
            male.setPairId(null);
            chickenRepository.save(male);
        }
        if (id.equals(female.getPairId())) {
            female.setPairId(null);
            chickenRepository.save(female);
        }

        breedingPairRepository.delete(pair);
        log.info("AUDIT: Breeding Pair Deleted. Code: {}", pair.getPairCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BreedingPairSummaryResponse> searchPairs(
            Long maleChickenId,
            Long femaleChickenId,
            PairStatus status,
            BreedingPurpose purpose,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Searching breeding pairs with filters");

        Specification<BreedingPair> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (maleChickenId != null) {
                predicates.add(cb.equal(root.get("maleChicken").get("id"), maleChickenId));
            }
            if (femaleChickenId != null) {
                predicates.add(cb.equal(root.get("femaleChicken").get("id"), femaleChickenId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (purpose != null) {
                predicates.add(cb.equal(root.get("breedingPurpose"), purpose));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return breedingPairRepository.findAll(spec, pageable).map(pairMapper::toSummaryResponse);
    }

    private void syncChickenPairIds(BreedingPair pair) {
        Chicken male = pair.getMaleChicken();
        Chicken female = pair.getFemaleChicken();
        
        if (pair.getStatus() == PairStatus.ACTIVE) {
            male.setPairId(pair.getId());
            female.setPairId(pair.getId());
        } else {
            // COMPLETED, CANCELLED, INACTIVE
            if (pair.getId().equals(male.getPairId())) {
                male.setPairId(null);
            }
            if (pair.getId().equals(female.getPairId())) {
                female.setPairId(null);
            }
        }
        
        chickenRepository.save(male);
        chickenRepository.save(female);
    }
}
