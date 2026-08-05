package com.poultry.backend.mapper;

import com.poultry.backend.dto.BreedingPairRequest;
import com.poultry.backend.dto.BreedingPairResponse;
import com.poultry.backend.dto.BreedingPairSummaryResponse;
import com.poultry.backend.dto.PairingHistoryResponse;
import com.poultry.backend.entity.BreedingPair;
import com.poultry.backend.entity.PairingType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class BreedingPairMapper {

    public BreedingPair toEntity(BreedingPairRequest request) {
        if (request == null) {
            return null;
        }
        return BreedingPair.builder()
                .pairCode(request.getPairCode())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .breedingPurpose(request.getBreedingPurpose() != null ? request.getBreedingPurpose() : com.poultry.backend.entity.BreedingPurpose.NATURAL_BREEDING)
                .pairingType(request.getPairingType() != null ? request.getPairingType() : PairingType.NATURAL)
                .expectedEggProduction(request.getExpectedEggProduction() != null ? request.getExpectedEggProduction() : 10)
                .remarks(request.getRemarks())
                .build();
    }

    public BreedingPairResponse toResponse(BreedingPair pair) {
        if (pair == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate endOrNow = pair.getEndDate() != null ? pair.getEndDate() : now;
        long daysSince = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), endOrNow) : 0;
        if (daysSince < 0) daysSince = 0;

        LocalDate expectedLaying = pair.getExpectedEggLayingDate() != null ? pair.getExpectedEggLayingDate() : 
                (pair.getStartDate() != null ? pair.getStartDate().plusDays(3) : null);

        LocalDate archiveDate = pair.getArchivedAt() != null ? pair.getArchivedAt().toLocalDate() :
                (pair.getEggLayingStartedAt() != null ? pair.getEggLayingStartedAt().toLocalDate().plusDays(2) : pair.getEndDate());

        return BreedingPairResponse.builder()
                .id(pair.getId())
                .pairCode(pair.getPairCode())

                .maleChickenId(pair.getMaleChicken() != null ? pair.getMaleChicken().getId() : null)
                .maleChickenCode(pair.getMaleChicken() != null ? pair.getMaleChicken().getChickenCode() : "")
                .maleChickenName(pair.getMaleChicken() != null ? pair.getMaleChicken().getName() : "")
                .maleChickenBreed(pair.getMaleChicken() != null && pair.getMaleChicken().getBreed() != null ? pair.getMaleChicken().getBreed().name() : "")
                .maleChickenPhotoUrl(pair.getMaleChicken() != null ? pair.getMaleChicken().getPhotoUrl() : null)

                .femaleChickenId(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getId() : null)
                .femaleChickenCode(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getChickenCode() : "")
                .femaleChickenName(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getName() : "")
                .femaleChickenBreed(pair.getFemaleChicken() != null && pair.getFemaleChicken().getBreed() != null ? pair.getFemaleChicken().getBreed().name() : "")
                .femaleChickenPhotoUrl(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getPhotoUrl() : null)

                .startDate(pair.getStartDate())
                .endDate(pair.getEndDate())
                .status(pair.getStatus())
                .breedingPurpose(pair.getBreedingPurpose())
                .pairingType(pair.getPairingType())
                .expectedEggProduction(pair.getExpectedEggProduction())
                .remarks(pair.getRemarks())

                .daysSincePairing(daysSince)
                .expectedEggLayingDate(expectedLaying)
                .eggLayingStartedAt(pair.getEggLayingStartedAt())
                .archivedAt(pair.getArchivedAt())
                .archiveDate(archiveDate)
                .elapsedDuration(daysSince + " Days")

                .createdAt(pair.getCreatedAt())
                .updatedAt(pair.getUpdatedAt())
                .build();
    }

    public BreedingPairSummaryResponse toSummaryResponse(BreedingPair pair) {
        if (pair == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate endOrNow = pair.getEndDate() != null ? pair.getEndDate() : now;
        long daysSince = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), endOrNow) : 0;
        if (daysSince < 0) daysSince = 0;

        LocalDate expectedLaying = pair.getExpectedEggLayingDate() != null ? pair.getExpectedEggLayingDate() :
                (pair.getStartDate() != null ? pair.getStartDate().plusDays(3) : null);

        LocalDate archiveDate = pair.getArchivedAt() != null ? pair.getArchivedAt().toLocalDate() :
                (pair.getEggLayingStartedAt() != null ? pair.getEggLayingStartedAt().toLocalDate().plusDays(2) : pair.getEndDate());

        return BreedingPairSummaryResponse.builder()
                .id(pair.getId())
                .pairCode(pair.getPairCode())

                .maleChickenId(pair.getMaleChicken() != null ? pair.getMaleChicken().getId() : null)
                .maleChickenCode(pair.getMaleChicken() != null ? pair.getMaleChicken().getChickenCode() : "")
                .maleChickenName(pair.getMaleChicken() != null ? pair.getMaleChicken().getName() : "")
                .maleChickenBreed(pair.getMaleChicken() != null && pair.getMaleChicken().getBreed() != null ? pair.getMaleChicken().getBreed().name() : "")
                .maleChickenPhotoUrl(pair.getMaleChicken() != null ? pair.getMaleChicken().getPhotoUrl() : null)

                .femaleChickenId(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getId() : null)
                .femaleChickenCode(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getChickenCode() : "")
                .femaleChickenName(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getName() : "")
                .femaleChickenBreed(pair.getFemaleChicken() != null && pair.getFemaleChicken().getBreed() != null ? pair.getFemaleChicken().getBreed().name() : "")
                .femaleChickenPhotoUrl(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getPhotoUrl() : null)

                .status(pair.getStatus())
                .breedingPurpose(pair.getBreedingPurpose())
                .pairingType(pair.getPairingType())
                .startDate(pair.getStartDate())
                .endDate(pair.getEndDate())

                .daysSincePairing(daysSince)
                .expectedEggLayingDate(expectedLaying)
                .eggLayingStartedAt(pair.getEggLayingStartedAt())
                .archiveDate(archiveDate)
                .build();
    }

    public PairingHistoryResponse toHistoryResponse(BreedingPair pair, int eggsProduced, int hatchBatches, int totalChicks) {
        if (pair == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate endOrNow = pair.getEndDate() != null ? pair.getEndDate() : now;
        long daysSince = pair.getStartDate() != null ? ChronoUnit.DAYS.between(pair.getStartDate(), endOrNow) : 0;
        if (daysSince < 0) daysSince = 0;

        LocalDate eggLayingDate = pair.getEggLayingStartedAt() != null ? pair.getEggLayingStartedAt().toLocalDate() : null;
        LocalDate archiveDate = pair.getArchivedAt() != null ? pair.getArchivedAt().toLocalDate() :
                (pair.getEggLayingStartedAt() != null ? pair.getEggLayingStartedAt().toLocalDate().plusDays(2) : pair.getEndDate());

        return PairingHistoryResponse.builder()
                .id(pair.getId())
                .pairCode(pair.getPairCode())

                .femaleChickenId(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getId() : null)
                .femaleChickenCode(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getChickenCode() : "")
                .femaleChickenName(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getName() : "")
                .femaleChickenBreed(pair.getFemaleChicken() != null && pair.getFemaleChicken().getBreed() != null ? pair.getFemaleChicken().getBreed().name() : "")
                .femaleChickenPhotoUrl(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getPhotoUrl() : null)

                .maleChickenId(pair.getMaleChicken() != null ? pair.getMaleChicken().getId() : null)
                .maleChickenCode(pair.getMaleChicken() != null ? pair.getMaleChicken().getChickenCode() : "")
                .maleChickenName(pair.getMaleChicken() != null ? pair.getMaleChicken().getName() : "")
                .maleChickenBreed(pair.getMaleChicken() != null && pair.getMaleChicken().getBreed() != null ? pair.getMaleChicken().getBreed().name() : "")
                .maleChickenPhotoUrl(pair.getMaleChicken() != null ? pair.getMaleChicken().getPhotoUrl() : null)

                .pairingType(pair.getPairingType())
                .pairingDate(pair.getStartDate())
                .eggLayingDate(eggLayingDate)
                .archiveDate(archiveDate)
                .duration(daysSince + " Days")

                .eggsProduced(eggsProduced)
                .hatchBatches(hatchBatches)
                .totalChicksBorn(totalChicks)

                .currentStatus(pair.getStatus())
                .remarks(pair.getRemarks())
                .createdAt(pair.getCreatedAt())
                .build();
    }
}
