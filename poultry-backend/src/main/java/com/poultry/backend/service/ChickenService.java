package com.poultry.backend.service;

import com.poultry.backend.dto.ChickenDashboardStatsResponse;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenOrigin;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChickenService {
    String generateNextChickenCode();
    ChickenResponse createChicken(ChickenRequest request);
    ChickenResponse getChickenById(Long id);
    ChickenResponse updateChicken(Long id, ChickenRequest request);
    ChickenResponse updateStatus(Long id, com.poultry.backend.dto.ChickenStatusPatchRequest request);
    List<com.poultry.backend.dto.ChickenTimelineEventDTO> getChickenTimeline(Long id);
    void deleteChicken(Long id);
    ChickenDashboardStatsResponse getDashboardStats();
    void bulkArchive(List<Long> ids);
    Page<ChickenSummaryResponse> searchChickens(
            String search,
            Breed breed,
            Gender gender,
            ChickenCategory category,
            ChickenStatus status,
            HealthStatus healthStatus,
            ChickenOrigin origin,
            String ageGroup,
            Integer minAgeDays,
            Integer maxAgeDays,
            Double minWeight,
            Double maxWeight,
            String chickenCode,
            String name,
            Pageable pageable
    );
    ChickenResponse updateWeight(Long id, com.poultry.backend.dto.ChickenActionDTOs.WeightUpdateRequest request);
    ChickenResponse transferChicken(Long id, com.poultry.backend.dto.ChickenActionDTOs.TransferRequest request);
    ChickenResponse sellChicken(Long id, com.poultry.backend.dto.ChickenActionDTOs.SellRequest request);
    void hardDeleteChicken(Long id);
    com.poultry.backend.dto.ChickenActionDTOs.ChickenFullProfileReportDTO getFullProfileReport(Long id);

    ChickenResponse pairChicken(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.PairingActionRequest request);
    ChickenResponse startHatchBatch(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.HatchBatchActionRequest request);
    ChickenResponse recordHatchResult(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.HatchResultActionRequest request);
    ChickenResponse moveToBrooding(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.BroodingActionRequest request);
    ChickenResponse markDeath(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.DeathRecordRequest request);
    ChickenResponse addExpense(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.ExpenseActionRequest request);
    ChickenResponse addFeedRecord(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.FeedRecordActionRequest request);
    ChickenResponse capturePhoto(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.PhotoCaptureRequest request);
    ChickenResponse archiveChicken(Long id);
    ChickenResponse restoreChicken(Long id);
    ChickenResponse assignWorker(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.WorkerAssignmentRequest request);
    ChickenResponse setReminder(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.ReminderActionRequest request);
    ChickenResponse addNote(Long id, com.poultry.backend.dto.ChickenAdvancedDTOs.ChickenNoteRequest request);

    com.poultry.backend.dto.ChickenAdvancedDTOs.AIHealthAnalysisResponse getAIAnalysis(Long id);
    com.poultry.backend.dto.ChickenAdvancedDTOs.BreedingPerformanceResponse getBreedingPerformance(Long id);
    com.poultry.backend.dto.ChickenAdvancedDTOs.MarketValueResponse calculateMarketValue(Long id);
    com.poultry.backend.dto.ChickenAdvancedDTOs.RelatedChickensResponse getRelatedChickens(Long id);
    List<com.poultry.backend.dto.ChickenAdvancedDTOs.ActivityItemDTO> getActivityLog(Long id);
    List<com.poultry.backend.dto.ChickenAdvancedDTOs.AuditItemDTO> getAuditHistory(Long id);
}

