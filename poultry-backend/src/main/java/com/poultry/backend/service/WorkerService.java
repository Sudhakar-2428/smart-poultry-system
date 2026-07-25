package com.poultry.backend.service;

import com.poultry.backend.dto.*;

import java.util.List;

public interface WorkerService {
    List<WorkerResponse> getWorkers(Long farmId, String currentUserEmail);
    WorkerResponse createWorker(Long farmId, WorkerRequest request, String currentUserEmail);
    WorkerInviteResponse inviteWorker(Long farmId, WorkerInviteRequest request, String currentUserEmail);
    AuthResponse joinFarmWithTempPassword(JoinFarmTempRequest request);
    WorkerResponse updateWorker(Long farmId, Long workerId, WorkerUpdateRequest request, String currentUserEmail);
    void deleteWorker(Long farmId, Long workerId, String currentUserEmail);
}
