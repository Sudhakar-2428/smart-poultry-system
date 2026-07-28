package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmBackupDTO {

    private String backupVersion;
    private Long farmId;
    private String farmName;
    private String exportedBy;
    private LocalDateTime exportedAt;

    private FarmResponse farm;
    private List<ChickenResponse> chickens;
    private List<FarmMemberResponse> workers;
    private List<EggRecordResponse> eggRecords;
    private List<HealthRecordResponse> healthRecords;
    private List<FeedConsumptionResponse> feedConsumptions;
    private List<FeedPurchaseResponse> feedPurchases;
    private List<LedgerTransactionResponse> financialTransactions;
    private List<SalesOrderResponse> salesOrders;
    private List<NotificationResponse> notifications;
    private Map<String, Object> settings;
}
