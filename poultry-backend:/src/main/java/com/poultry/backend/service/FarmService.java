package com.poultry.backend.service;

import com.poultry.backend.dto.FarmRequest;
import com.poultry.backend.dto.FarmResponse;

import java.util.List;

public interface FarmService {
    FarmResponse createFarm(FarmRequest request, String currentUserEmail);
    FarmResponse getFarmById(Long id);
    FarmResponse getFarmByUniqueId(String farmUniqueId);
    List<FarmResponse> getMyFarms(String currentUserEmail);
    String regenerateJoinCode(String farmUniqueId, String currentUserEmail);
    FarmResponse updateFarmLocation(Long farmId, com.poultry.backend.dto.FarmLocationUpdateRequest request, String currentUserEmail);
}
