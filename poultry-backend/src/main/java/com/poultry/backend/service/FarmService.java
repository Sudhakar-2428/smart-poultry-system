package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FarmService {
    FarmResponse createFarm(FarmRequest request, String currentUserEmail);
    FarmResponse getFarmById(Long id);
    FarmResponse getFarmByUniqueId(String farmUniqueId);
    List<FarmResponse> getMyFarms(String currentUserEmail);
    String regenerateJoinCode(String farmUniqueId, String currentUserEmail);
    FarmResponse updateFarmLocation(Long farmId, FarmLocationUpdateRequest request, String currentUserEmail);
    
    FarmProfileResponse getFarmProfile(Long farmId, String currentUserEmail);
    FarmProfileResponse updateFarmProfile(Long farmId, FarmProfileUpdateRequest request, String currentUserEmail);
    FarmProfileResponse uploadFarmLogo(Long farmId, MultipartFile file, String currentUserEmail);
    FarmProfileResponse deleteFarmLogo(Long farmId, String currentUserEmail);
}
