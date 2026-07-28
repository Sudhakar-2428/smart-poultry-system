package com.poultry.backend.service;

import com.poultry.backend.dto.DangerZoneActionRequest;
import com.poultry.backend.dto.DangerZoneResponse;
import com.poultry.backend.dto.DeleteFarmRequest;
import com.poultry.backend.dto.FarmBackupDTO;

public interface DangerZoneService {

    DangerZoneResponse deleteFarm(Long farmId, DeleteFarmRequest request);

    DangerZoneResponse removeAllChickenData(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse removeAllEggData(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse removeAllHealthRecords(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse removeAllFeedRecords(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse removeAllFinancialRecords(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse removeAllReports(Long farmId, DangerZoneActionRequest request);

    DangerZoneResponse resetFarmSettings(Long farmId, DangerZoneActionRequest request);

    FarmBackupDTO exportFarmBackup(Long farmId);

    DangerZoneResponse importFarmBackup(Long farmId, FarmBackupDTO backupData, DangerZoneActionRequest request);
}
