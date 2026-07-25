package com.poultry.backend.service;

import com.poultry.backend.dto.DeleteFarmRequest;
import com.poultry.backend.dto.DeleteFarmResponse;
import com.poultry.backend.dto.FarmDeleteCheckResponse;

public interface DeleteFarmService {
    FarmDeleteCheckResponse checkDeleteEligibility(Long farmId);
    DeleteFarmResponse deleteFarm(Long farmId, DeleteFarmRequest request);
}
