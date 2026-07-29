package com.poultry.backend.service.impl;

import com.poultry.backend.dto.NotificationRequest;
import com.poultry.backend.dto.NotificationResponse;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.NotificationMapper;
import com.poultry.backend.repository.NotificationRepository;
import com.poultry.backend.service.NotificationService;
import com.poultry.backend.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with ID: " + id));
        checkRecipientRoleAccess(notification);
        return notificationMapper.toResponse(notification);
    }

    private Role getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth.startsWith("ROLE_")) {
                    String roleName = auth.substring(5);
                    try {
                        return Role.valueOf(roleName);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return SecurityUtils.getCurrentUserDetails()
                .map(details -> details.getUser().getRole())
                .orElse(null);
    }

    private List<RecipientRole> getAllowedRecipientRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<RecipientRole> roles = new ArrayList<>();
        roles.add(RecipientRole.ALL);
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth.startsWith("ROLE_")) {
                    String roleName = auth.substring(5);
                    try {
                        RecipientRole rr = RecipientRole.valueOf(roleName);
                        if (!roles.contains(rr)) {
                            roles.add(rr);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return roles;
    }

    private void checkRecipientRoleAccess(Notification notification) {
        if (notification.getRecipientRole() == RecipientRole.ALL) {
            return;
        }
        List<RecipientRole> allowed = getAllowedRecipientRoles();
        if (!allowed.contains(notification.getRecipientRole())) {
            throw new AccessDeniedException("You do not have permission to access notifications intended for role: " + notification.getRecipientRole());
        }
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(request.getNotificationType())
                .severity(request.getSeverity())
                .sourceModule(request.getSourceModule())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .recipientRole(request.getRecipientRole())
                .isRead(false)
                .isArchived(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("AUDIT: Notification Created. ID: {}, Title: {}", saved.getId(), saved.getTitle());
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with ID: " + id));

        checkRecipientRoleAccess(notification);

        if (notification.isArchived()) {
            throw new ValidationException("Archived notifications cannot be modified.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        log.info("AUDIT: Notification Read. ID: {}", notification.getId());
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse archiveNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with ID: " + id));

        checkRecipientRoleAccess(notification);

        if (notification.isArchived()) {
            throw new ValidationException("Archived notifications cannot be modified.");
        }

        notification.setArchived(true);
        notification.setArchivedAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        log.info("AUDIT: Notification Archived. ID: {}", notification.getId());
        return notificationMapper.toResponse(notification);
    }

    @Override
    public long getUnreadCount() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        return notificationRepository.countByIsReadFalseAndIsArchivedFalseAndRecipientRoleIn(roles);
    }

    @Override
    public Page<NotificationResponse> searchNotifications(
            NotificationType type,
            Severity severity,
            SourceModule sourceModule,
            RecipientRole recipientRole,
            Boolean isRead,
            Boolean isArchived,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        List<RecipientRole> allowedRoles = getAllowedRecipientRoles();

        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule constraint: Only recipient roles should access their notifications
            predicates.add(root.get("recipientRole").in(allowedRoles));

            if (type != null) {
                predicates.add(cb.equal(root.get("notificationType"), type));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (sourceModule != null) {
                predicates.add(cb.equal(root.get("sourceModule"), sourceModule));
            }
            if (recipientRole != null) {
                predicates.add(cb.equal(root.get("recipientRole"), recipientRole));
            }
            if (isRead != null) {
                predicates.add(cb.equal(root.get("isRead"), isRead));
            }
            if (isArchived != null) {
                predicates.add(cb.equal(root.get("isArchived"), isArchived));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toResponse);
    }

    @Override
    @Transactional
    public int bulkRead() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        LocalDateTime readAt = LocalDateTime.now();
        int updated = notificationRepository.markAllAsReadForRoles(roles, readAt);
        log.info("AUDIT: Bulk Read Executed. Count: {}, Roles: {}", updated, roles);
        return updated;
    }

    @Override
    public List<NotificationResponse> getRecentNotifications(int limit) {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        Pageable limitPage = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("createdAt")));
        
        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> cb.and(
                root.get("recipientRole").in(roles),
                cb.equal(root.get("isArchived"), false)
        );

        return notificationRepository.findAll(spec, limitPage).getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> getCriticalNotifications() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> cb.and(
                root.get("recipientRole").in(roles),
                cb.equal(root.get("isArchived"), false),
                cb.equal(root.get("severity"), Severity.CRITICAL)
        );

        return notificationRepository.findAll(spec).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> cb.and(
                root.get("recipientRole").in(roles),
                cb.equal(root.get("isArchived"), false),
                cb.equal(root.get("isRead"), false)
        );

        return notificationRepository.findAll(spec).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    public Map<String, Long> getNotificationCountBySeverity() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> cb.and(
                root.get("recipientRole").in(roles),
                cb.equal(root.get("isArchived"), false)
        );

        List<Notification> active = notificationRepository.findAll(spec);
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            countMap.put(s.name(), 0L);
        }
        for (Notification n : active) {
            countMap.put(n.getSeverity().name(), countMap.getOrDefault(n.getSeverity().name(), 0L) + 1);
        }
        return countMap;
    }

    @Override
    public Map<String, Long> getNotificationCountByModule() {
        List<RecipientRole> roles = getAllowedRecipientRoles();
        org.springframework.data.jpa.domain.Specification<Notification> spec = (root, query, cb) -> cb.and(
                root.get("recipientRole").in(roles),
                cb.equal(root.get("isArchived"), false)
        );

        List<Notification> active = notificationRepository.findAll(spec);
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (SourceModule m : SourceModule.values()) {
            countMap.put(m.name(), 0L);
        }
        for (Notification n : active) {
            countMap.put(n.getSourceModule().name(), countMap.getOrDefault(n.getSourceModule().name(), 0L) + 1);
        }
        return countMap;
    }
}
