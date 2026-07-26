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
    private final com.poultry.backend.repository.ChickenTimelineRepository chickenTimelineRepository;

    private void recordTimelineEvent(Chicken chicken, String eventType, String title, String description) {
        try {
            com.poultry.backend.entity.ChickenTimelineEvent event = com.poultry.backend.entity.ChickenTimelineEvent.builder()
                    .chicken(chicken)
                    .eventType(eventType)
                    .title(title)
                    .description(description)
                    .createdBy("System Admin")
                    .build();
            chickenTimelineRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record timeline event for chicken {}: {}", chicken.getChickenCode(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextChickenCode() {
        Long maxId = chickenRepository.getMaxChickenId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String candidateCode = String.format("CHK-%06d", nextId);
        while (chickenRepository.existsByChickenCode(candidateCode)) {
            nextId++;
            candidateCode = String.format("CHK-%06d", nextId);
        }
        return candidateCode;
    }

    @Override
    @Transactional
    public ChickenResponse createChicken(ChickenRequest request) {
        log.info("Processing instruction to register new chicken. Code: {}", request.getChickenCode());

        // 1. Auto-generate Chicken Code if blank/null
        if (request.getChickenCode() == null || request.getChickenCode().isBlank()) {
            request.setChickenCode(generateNextChickenCode());
        } else if (chickenRepository.existsByChickenCode(request.getChickenCode().trim())) {
            throw new DuplicateRecordException("Chicken code '" + request.getChickenCode() + "' is already registered.");
        }

        // 2. Validate Weight > 0
        if (request.getWeight() != null && request.getWeight() <= 0) {
            throw new ValidationException("Chicken weight must be greater than 0 kg.");
        }

        // 3. Validate DOB cannot be in the future
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        // 4. Validate Purchase Date & Cost
        if (request.getPurchaseDate() != null && request.getDateOfBirth() != null && request.getPurchaseDate().isBefore(request.getDateOfBirth())) {
            throw new ValidationException("Purchase date cannot be before date of birth.");
        }
        if (request.getPurchaseCost() != null && request.getPurchaseCost() < 0) {
            throw new ValidationException("Purchase cost cannot be negative.");
        }

        // 5. Validate Father & Mother
        if (request.getFatherId() != null && request.getMotherId() != null && request.getFatherId().equals(request.getMotherId())) {
            throw new ValidationException("Father chicken and mother chicken cannot be the same chicken.");
        }

        Chicken chicken = chickenMapper.toEntity(request);
        Chicken savedChicken = chickenRepository.save(chicken);

        recordTimelineEvent(savedChicken, "REGISTRATION", "Chicken Registered",
                "Registered chicken " + savedChicken.getChickenCode() + " (" + savedChicken.getBreed() + ", " + savedChicken.getCategory() + ")");

        log.info("AUDIT: Chicken registration processed. Code: {}, Breed: {}, Category: {}, Status: {}",
                savedChicken.getChickenCode(), savedChicken.getBreed(), savedChicken.getCategory(), savedChicken.getStatus());

        return getChickenById(savedChicken.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ChickenResponse getChickenById(Long id) {
        log.info("Retrieving details of chicken for ID: {}", id);
        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        ChickenResponse response = chickenMapper.toResponse(chicken);

        // Map timeline events
        List<com.poultry.backend.entity.ChickenTimelineEvent> events = chickenTimelineRepository.findByChickenIdOrderByTimestampDesc(id);
        List<com.poultry.backend.dto.ChickenTimelineEventDTO> timelineDtos = events.stream()
                .map(chickenMapper::toTimelineDTO)
                .toList();
        response.setTimeline(timelineDtos);

        return response;
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
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        Double oldWeight = chicken.getWeight();
        com.poultry.backend.entity.HealthStatus oldHealth = chicken.getHealthStatus();
        ChickenStatus oldStatus = chicken.getStatus();

        chickenMapper.updateEntityFromRequest(request, chicken);
        Chicken updatedChicken = chickenRepository.save(chicken);

        if (oldWeight != null && request.getWeight() != null && !oldWeight.equals(request.getWeight())) {
            recordTimelineEvent(updatedChicken, "WEIGHT_UPDATE", "Weight Updated",
                    "Weight recorded from " + oldWeight + " kg to " + request.getWeight() + " kg");
        }
        if (oldHealth != updatedChicken.getHealthStatus()) {
            recordTimelineEvent(updatedChicken, "HEALTH_UPDATE", "Health Status Updated",
                    "Health status updated to " + updatedChicken.getHealthStatus());
        }
        if (oldStatus != updatedChicken.getStatus()) {
            recordTimelineEvent(updatedChicken, "STATUS_CHANGE", "Status Changed",
                    "Status updated from " + oldStatus + " to " + updatedChicken.getStatus());
        } else {
            recordTimelineEvent(updatedChicken, "PROFILE_UPDATE", "Profile Updated", "Chicken profile information was updated.");
        }

        log.info("AUDIT: Chicken update processed. ID: {}, Code: {}", id, updatedChicken.getChickenCode());

        return getChickenById(id);
    }

    @Override
    @Transactional
    public ChickenResponse updateStatus(Long id, com.poultry.backend.dto.ChickenStatusPatchRequest request) {
        log.info("Processing PATCH status for chicken ID: {}. New status: {}", id, request.getStatus());
        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        ChickenStatus oldStatus = chicken.getStatus();
        chicken.setStatus(request.getStatus());

        if (request.getHealthStatus() != null) {
            chicken.setHealthStatus(request.getHealthStatus());
        }
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            chicken.setRemarks(request.getRemarks());
        }

        Chicken saved = chickenRepository.save(chicken);

        recordTimelineEvent(saved, "STATUS_CHANGE", "Status Updated",
                "Status changed from " + oldStatus + " to " + request.getStatus() + (request.getRemarks() != null ? " (" + request.getRemarks() + ")" : ""));

        return getChickenById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.poultry.backend.dto.ChickenTimelineEventDTO> getChickenTimeline(Long id) {
        log.info("Retrieving timeline events for chicken ID: {}", id);
        if (!chickenRepository.existsById(id)) {
            throw new NotFoundException("Chicken not found with ID: " + id);
        }
        return chickenTimelineRepository.findByChickenIdOrderByTimestampDesc(id).stream()
                .map(chickenMapper::toTimelineDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteChicken(Long id) {
        log.info("Processing instruction to soft delete chicken. ID: {}", id);

        Chicken chicken = chickenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + id));

        if (chicken.getStatus() == ChickenStatus.SOLD) {
            throw new ValidationException("Cannot delete SOLD chickens.");
        }
        if (chicken.getStatus() == ChickenStatus.DEAD) {
            throw new ValidationException("Cannot delete DEAD chickens.");
        }

        chicken.setStatus(ChickenStatus.INACTIVE);
        chickenRepository.save(chicken);

        recordTimelineEvent(chicken, "ARCHIVED", "Chicken Soft Deleted / Archived", "Chicken record was soft deleted and marked INACTIVE.");

        log.info("AUDIT: Chicken soft deletion processed. ID: {}, Code: {}", id, chicken.getChickenCode());
    }

    @Override
    @Transactional(readOnly = true)
    public com.poultry.backend.dto.ChickenDashboardStatsResponse getDashboardStats() {
        log.info("Computing dashboard statistics metrics for chicken flock registry");

        try {
            long total = chickenRepository.count();
            long healthy = chickenRepository.countByHealthStatus(com.poultry.backend.entity.HealthStatus.HEALTHY);
            long sick = chickenRepository.countByHealthStatus(com.poultry.backend.entity.HealthStatus.SICK)
                    + chickenRepository.countByHealthStatus(com.poultry.backend.entity.HealthStatus.UNDER_TREATMENT)
                    + chickenRepository.countByHealthStatus(com.poultry.backend.entity.HealthStatus.OBSERVATION);
            long sold = chickenRepository.countByStatus(ChickenStatus.SOLD);
            long dead = chickenRepository.countByStatus(ChickenStatus.DEAD)
                    + chickenRepository.countByHealthStatus(com.poultry.backend.entity.HealthStatus.DECEASED);
            long hens = chickenRepository.countByGender(Gender.FEMALE);
            long roosters = chickenRepository.countByGender(Gender.MALE);
            long country = chickenRepository.countByCategory(ChickenCategory.COUNTRY_CHICKEN);
            long broilers = chickenRepository.countByCategory(ChickenCategory.BROILER);
            long layers = chickenRepository.countByCategory(ChickenCategory.LAYER);
            long recentlyRegistered = chickenRepository.countByCreatedAtAfter(java.time.LocalDateTime.now().minusDays(30));

            return com.poultry.backend.dto.ChickenDashboardStatsResponse.builder()
                    .totalChickens(total)
                    .healthy(healthy)
                    .sick(sick)
                    .sold(sold)
                    .dead(dead)
                    .hens(hens)
                    .roosters(roosters)
                    .countryChickens(country)
                    .broilers(broilers)
                    .layers(layers)
                    .recentlyRegistered(recentlyRegistered)
                    .build();
        } catch (Exception e) {
            log.error("Exception computing chicken dashboard stats, returning zero metrics", e);
            return com.poultry.backend.dto.ChickenDashboardStatsResponse.builder()
                    .totalChickens(0L)
                    .healthy(0L)
                    .sick(0L)
                    .sold(0L)
                    .dead(0L)
                    .hens(0L)
                    .roosters(0L)
                    .countryChickens(0L)
                    .broilers(0L)
                    .layers(0L)
                    .recentlyRegistered(0L)
                    .build();
        }
    }

    @Override
    @Transactional
    public void bulkArchive(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        log.info("Processing bulk archive action for {} chicken IDs", ids.size());
        List<Chicken> chickens = chickenRepository.findAllById(ids);
        for (Chicken chicken : chickens) {
            chicken.setStatus(ChickenStatus.SOLD);
        }
        chickenRepository.saveAll(chickens);
        log.info("AUDIT: Bulk archive successfully updated {} chickens to SOLD/ARCHIVED state", chickens.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChickenSummaryResponse> searchChickens(
            String search,
            Breed breed,
            Gender gender,
            ChickenCategory category,
            ChickenStatus status,
            com.poultry.backend.entity.HealthStatus healthStatus,
            com.poultry.backend.entity.ChickenOrigin origin,
            String ageGroup,
            Integer minAgeDays,
            Integer maxAgeDays,
            Double minWeight,
            Double maxWeight,
            String chickenCode,
            String name,
            Pageable pageable
    ) {
        log.info("Searching chickens with dynamic filters. Search: {}, Sort: {}", search, pageable.getSort());

        Integer calcMinAge = minAgeDays;
        Integer calcMaxAge = maxAgeDays;
        if (ageGroup != null && !ageGroup.trim().isEmpty()) {
            String ag = ageGroup.trim().toLowerCase();
            if ("chick".equals(ag)) {
                calcMaxAge = 60;
            } else if ("grower".equals(ag)) {
                calcMinAge = 61;
                calcMaxAge = 150;
            } else if ("adult".equals(ag)) {
                calcMinAge = 151;
            }
        }
        final Integer finalMinAge = calcMinAge;
        final Integer finalMaxAge = calcMaxAge;

        Specification<Chicken> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Unified search query over Chicken ID, Breed, Wing Tag, Leg Band, Name
            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate matchCode = cb.like(cb.lower(root.get("chickenCode")), term);
                Predicate matchName = cb.like(cb.lower(root.get("name")), term);
                Predicate matchWingTag = cb.like(cb.lower(root.get("wingTagNumber")), term);
                Predicate matchLegBand = cb.like(cb.lower(root.get("legBandNumber")), term);
                
                // Match breed enum string name
                Predicate matchBreed = cb.like(cb.lower(root.get("breed").as(String.class)), term);

                predicates.add(cb.or(matchCode, matchName, matchWingTag, matchLegBand, matchBreed));
            }

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
            if (healthStatus != null) {
                predicates.add(cb.equal(root.get("healthStatus"), healthStatus));
            }
            if (origin != null) {
                predicates.add(cb.equal(root.get("origin"), origin));
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
            if (finalMinAge != null) {
                LocalDate oldestDob = LocalDate.now().minusDays(finalMinAge);
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), oldestDob));
            }
            if (finalMaxAge != null) {
                LocalDate youngestDob = LocalDate.now().minusDays(finalMaxAge);
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfBirth"), youngestDob));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return chickenRepository.findAll(spec, pageable).map(chickenMapper::toSummaryResponse);
    }
}
