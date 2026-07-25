package com.poultry.backend.service;

import com.poultry.backend.dto.WorkerRequest;
import com.poultry.backend.dto.WorkerResponse;
import com.poultry.backend.dto.WorkerUpdateRequest;

import java.util.List;

public interface WorkerService {
    List<WorkerResponse> getWorkers(Long farmId, String currentUserEmail);
    WorkerResponse createWorker(Long farmId, WorkerRequest request, String currentUserEmail);
    WorkerResponse updateWorker(Long farmId, Long workerId, WorkerUpdateRequest request, String currentUserEmail);
    void deleteWorker(Long farmId, Long workerId, String currentUserEmail);
}
