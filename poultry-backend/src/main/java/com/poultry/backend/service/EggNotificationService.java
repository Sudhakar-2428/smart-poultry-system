package com.poultry.backend.service;

import com.poultry.backend.dto.EggNotificationDTOs;

import java.util.List;

public interface EggNotificationService {
    void triggerDaily08AMNotifications();
    List<EggNotificationDTOs.EggNotificationResponse> getActivePendingNotifications();
    List<EggNotificationDTOs.EggNotificationResponse> getNotificationsByStatus(String status);
    EggNotificationDTOs.EggNotificationResponse confirmEggCollection(Long notificationId, EggNotificationDTOs.ConfirmEggCollectionRequest request, String currentUser);
    EggNotificationDTOs.EggNotificationResponse recordNoEgg(Long notificationId, EggNotificationDTOs.NoEggReasonRequest request, String currentUser);
    EggNotificationDTOs.EggNotificationResponse rescheduleNotification(Long notificationId, EggNotificationDTOs.RescheduleNotificationRequest request, String currentUser);
    void process06PMEscalation();
    void process07PMManagerEmailAlert();
    EggNotificationDTOs.EggNotificationReportDTO getNotificationReport(String status, String startDate, String endDate);
}
