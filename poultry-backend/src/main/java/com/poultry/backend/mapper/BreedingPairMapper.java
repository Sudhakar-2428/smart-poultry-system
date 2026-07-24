package com.poultry.backend.mapper;

import com.poultry.backend.dto.BreedingPairRequest;
import com.poultry.backend.dto.BreedingPairResponse;
import com.poultry.backend.dto.BreedingPairSummaryResponse;
import com.poultry.backend.entity.BreedingPair;
import org.springframework.stereotype.Component;

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
                .breedingPurpose(request.getBreedingPurpose())
                .expectedEggProduction(request.getExpectedEggProduction())
                .remarks(request.getRemarks())
                .build();
    }

    public BreedingPairResponse toResponse(BreedingPair pair) {
        if (pair == null) {
            return null;
        }
        return BreedingPairResponse.builder()
                .id(pair.getId())
                .pairCode(pair.getPairCode())
                .maleChickenId(pair.getMaleChicken() != null ? pair.getMaleChicken().getId() : null)
                .maleChickenCode(pair.getMaleChicken() != null ? pair.getMaleChicken().getChickenCode() : "")
                .femaleChickenId(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getId() : null)
                .femaleChickenCode(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getChickenCode() : "")
                .startDate(pair.getStartDate())
                .endDate(pair.getEndDate())
                .status(pair.getStatus())
                .breedingPurpose(pair.getBreedingPurpose())
                .expectedEggProduction(pair.getExpectedEggProduction())
                .remarks(pair.getRemarks())
                .createdAt(pair.getCreatedAt())
                .updatedAt(pair.getUpdatedAt())
                .build();
    }

    public BreedingPairSummaryResponse toSummaryResponse(BreedingPair pair) {
        if (pair == null) {
            return null;
        }
        return BreedingPairSummaryResponse.builder()
                .id(pair.getId())
                .pairCode(pair.getPairCode())
                .maleChickenId(pair.getMaleChicken() != null ? pair.getMaleChicken().getId() : null)
                .maleChickenCode(pair.getMaleChicken() != null ? pair.getMaleChicken().getChickenCode() : "")
                .femaleChickenId(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getId() : null)
                .femaleChickenCode(pair.getFemaleChicken() != null ? pair.getFemaleChicken().getChickenCode() : "")
                .status(pair.getStatus())
                .breedingPurpose(pair.getBreedingPurpose())
                .startDate(pair.getStartDate())
                .endDate(pair.getEndDate())
                .build();
    }
}
