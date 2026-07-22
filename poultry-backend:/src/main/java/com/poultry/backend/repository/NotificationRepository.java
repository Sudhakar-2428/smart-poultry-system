package com.poultry.backend.repository;

import com.poultry.backend.entity.Notification;
import com.poultry.backend.entity.RecipientRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByTargetId(Long targetId);
    List<Notification> findByType(String type);

    long countByIsReadFalseAndIsArchivedFalseAndRecipientRoleIn(Collection<RecipientRole> roles);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.isRead = false AND n.isArchived = false AND n.recipientRole IN :roles")
    int markAllAsReadForRoles(@Param("roles") Collection<RecipientRole> roles, @Param("readAt") LocalDateTime readAt);

    // Dashboard metrics
    List<Notification> findTop10ByRecipientRoleInAndIsArchivedFalseOrderByCreatedAtDesc(Collection<RecipientRole> roles);
}
