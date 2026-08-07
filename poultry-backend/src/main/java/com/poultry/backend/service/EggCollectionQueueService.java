package com.poultry.backend.service;

import com.poultry.backend.dto.EggCollectionQueueDTOs;

import java.util.List;

public interface EggCollectionQueueService {
    EggCollectionQueueDTOs.EggQueueSummaryResponse getTodayQueue(String currentUser);
    EggCollectionQueueDTOs.EggQueueSummaryResponse generateTodayQueue();
    EggCollectionQueueDTOs.EggQueueItemResponse confirmQueueItem(Long itemId, EggCollectionQueueDTOs.ConfirmQueueItemRequest request, String currentUser);
    EggCollectionQueueDTOs.EggQueueItemResponse noEggQueueItem(Long itemId, EggCollectionQueueDTOs.NoEggQueueItemRequest request, String currentUser);
    EggCollectionQueueDTOs.EggQueueItemResponse rescheduleQueueItem(Long itemId, EggCollectionQueueDTOs.RescheduleQueueItemRequest request, String currentUser);
    EggCollectionQueueDTOs.EggQueueSummaryResponse getQueueReport(String status, String breed);
}
