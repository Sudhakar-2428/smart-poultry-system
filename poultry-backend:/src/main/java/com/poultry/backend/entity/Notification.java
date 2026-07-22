package com.poultry.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", nullable = false, length = 30)
    private SourceModule sourceModule;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 30)
    private RecipientRole recipientRole;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- Backward Compatibility Fields ---
    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 50)
    private String type; // mapped to target type if needed

    @PrePersist
    public void prePersist() {
        if (isRead) {
            if (readAt == null) {
                readAt = LocalDateTime.now();
            }
        }
        if (isArchived) {
            if (archivedAt == null) {
                archivedAt = LocalDateTime.now();
            }
        }

        // Deduce fields from old builder calls if they are not set
        if (notificationType == null) {
            if (type != null) {
                String upperType = type.toUpperCase();
                if (upperType.contains("VACCINATION") || upperType.contains("DISEASE") || upperType.contains("DECEASED")) {
                    notificationType = NotificationType.HEALTH;
                } else if (upperType.contains("STOCK") || upperType.contains("EXPIRY") || upperType.contains("FEED")) {
                    notificationType = NotificationType.FEED;
                } else if (upperType.contains("FINANCE") || upperType.contains("EXPENSE") || upperType.contains("INCOME") || upperType.contains("BALANCE")) {
                    notificationType = NotificationType.FINANCE;
                } else if (upperType.contains("SALE") || upperType.contains("PAYMENT") || upperType.contains("ORDER")) {
                    notificationType = NotificationType.SALES;
                } else {
                    notificationType = NotificationType.SYSTEM;
                }
            } else {
                notificationType = NotificationType.SYSTEM;
            }
        }

        if (severity == null) {
            if (type != null) {
                String upperType = type.toUpperCase();
                if (upperType.contains("CRITICAL")) {
                    severity = Severity.CRITICAL;
                } else if (upperType.contains("ERROR") || upperType.contains("DECEASED") || upperType.contains("OUT_OF_STOCK")) {
                    severity = Severity.ERROR;
                } else if (upperType.contains("WARNING") || upperType.contains("OVERDUE") || upperType.contains("DUE") || upperType.contains("ALERT")) {
                    severity = Severity.WARNING;
                } else {
                    severity = Severity.INFO;
                }
            } else {
                severity = Severity.INFO;
            }
        }

        if (sourceModule == null) {
            switch (notificationType) {
                case HEALTH: sourceModule = SourceModule.HEALTH; break;
                case FEED: sourceModule = SourceModule.FEED; break;
                case SALES: sourceModule = SourceModule.SALES; break;
                case FINANCE: sourceModule = SourceModule.FINANCE; break;
                case REPORT: sourceModule = SourceModule.REPORTS; break;
                default: sourceModule = SourceModule.SYSTEM; break;
            }
        }

        if (recipientRole == null) {
            if (type != null) {
                String upperType = type.toUpperCase();
                if (upperType.contains("VACCINATION") || upperType.contains("DISEASE")) {
                    recipientRole = RecipientRole.VETERINARIAN;
                } else if (upperType.contains("FINANCE") || upperType.contains("EXPENSE") || upperType.contains("INCOME") || upperType.contains("BALANCE") || upperType.contains("SALE")) {
                    recipientRole = RecipientRole.ADMIN;
                } else if (upperType.contains("STOCK") || upperType.contains("LIMIT") || upperType.contains("OUTSTANDING")) {
                    recipientRole = RecipientRole.MANAGER;
                } else if (upperType.contains("EXPIRY") || upperType.contains("FEED")) {
                    recipientRole = RecipientRole.WORKER;
                } else {
                    recipientRole = RecipientRole.ALL;
                }
            } else {
                recipientRole = RecipientRole.ALL;
            }
        }

        if (referenceType == null) {
            if (type != null) {
                String upperType = type.toUpperCase();
                if (upperType.contains("VACCINATION") || upperType.contains("DISEASE") || upperType.contains("DECEASED")) {
                    referenceType = "CHICKEN";
                } else if (upperType.contains("STOCK") || upperType.contains("EXPIRY") || upperType.contains("FEED")) {
                    referenceType = "FEED_ITEM";
                } else if (upperType.contains("FINANCE") || upperType.contains("EXPENSE") || upperType.contains("INCOME")) {
                    referenceType = "LEDGER_TRANSACTION";
                } else if (upperType.contains("BALANCE")) {
                    referenceType = "LEDGER_ACCOUNT";
                } else if (upperType.contains("SALE") || upperType.contains("ORDER") || upperType.contains("PAYMENT")) {
                    referenceType = "SALES_ORDER";
                } else {
                    referenceType = "SYSTEM";
                }
            } else {
                referenceType = "SYSTEM";
            }
        }

        if (referenceId == null && targetId != null) {
            referenceId = targetId;
        }

        if (title == null) {
            if (type != null) {
                // capitalize type beautifully
                String formatted = type.replace("_", " ").toLowerCase();
                if (!formatted.isEmpty()) {
                    title = Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
                } else {
                    title = "Notification Alert";
                }
            } else {
                title = "System Notification";
            }
        }

        // Keep compatibility fields updated
        if (targetId == null && referenceId != null) {
            targetId = referenceId;
        }
        if (type == null && notificationType != null) {
            type = notificationType.name();
        }
    }
}
