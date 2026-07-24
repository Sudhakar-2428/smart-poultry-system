package com.poultry.backend.mapper;

import com.poultry.backend.dto.ChickGrowthRequest;
import com.poultry.backend.dto.ChickGrowthResponse;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.FarmSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class ChickGrowthMapper {

    private final FarmSettingRepository farmSettingRepository;

    public ChickGrowthRecord toEntity(ChickGrowthRequest request) {
        if (request == null) {
            return null;
        }
        return ChickGrowthRecord.builder()
                .growthDate(request.getGrowthDate())
                .weight(request.getWeight())
                .height(request.getHeight())
                .healthStatus(request.getHealthStatus())
                .growthStage(request.getGrowthStage())
                .remarks(request.getRemarks())
                .build();
    }

    public ChickGrowthResponse toResponse(ChickGrowthRecord record) {
        if (record == null) {
            return null;
        }

        Chicken chicken = record.getChicken();
        int currentAge = 0;
        Gender chickenGender = Gender.UNKNOWN;
        String chickenCode = "";

        if (chicken != null) {
            chickenCode = chicken.getChickenCode();
            chickenGender = chicken.getGender();
            if (chicken.getDateOfBirth() != null) {
                currentAge = (int) ChronoUnit.DAYS.between(chicken.getDateOfBirth(), LocalDate.now());
            }
        }

        int transitionAge = 150;
        try {
            transitionAge = farmSettingRepository.findById("ADULT_TRANSITION_AGE")
                    .map(setting -> Integer.parseInt(setting.getValue()))
                    .orElse(150);
        } catch (Exception e) {
            // fallback
        }

        int daysRemaining = Math.max(0, transitionAge - currentAge);
        double progress = transitionAge > 0
                ? ((double) currentAge / transitionAge) * 100.0
                : 0.0;
        progress = Math.min(100.0, Math.max(0.0, progress));

        GrowthStage currentStage = record.getGrowthStage();
        if (chicken != null) {
            if (chicken.getStatus() == ChickenStatus.ACTIVE || chicken.getCategory() != ChickenCategory.CHICK) {
                currentStage = GrowthStage.ADULT;
            } else {
                if (currentAge < 14) {
                    currentStage = GrowthStage.BROODER;
                } else if (currentAge < 42) {
                    currentStage = GrowthStage.STARTER;
                } else if (currentAge < 90) {
                    currentStage = GrowthStage.GROWER;
                } else {
                    currentStage = chickenGender == Gender.MALE ? GrowthStage.COCKEREL : GrowthStage.PULLET;
                }
            }
        }

        return ChickGrowthResponse.builder()
                .id(record.getId())
                .chickenId(chicken != null ? chicken.getId() : null)
                .chickenCode(chickenCode)
                .growthDate(record.getGrowthDate())
                .ageInDays(record.getAgeInDays())
                .weight(record.getWeight())
                .height(record.getHeight())
                .healthStatus(record.getHealthStatus())
                .growthStage(record.getGrowthStage())
                .gender(record.getGender() != null ? record.getGender() : chickenGender)
                .remarks(record.getRemarks())
                .currentAge(currentAge)
                .growthProgressPct(progress)
                .daysUntilAdultTransition(daysRemaining)
                .currentGrowthStage(currentStage)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
