package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Notification notifAdmin;
    private Notification notifVet;
    private Notification notifAll;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        // 1. Create ADMIN Notification
        notifAdmin = Notification.builder()
                .title("Admin Alert")
                .message("High level security update required.")
                .notificationType(NotificationType.SYSTEM)
                .severity(Severity.CRITICAL)
                .sourceModule(SourceModule.SYSTEM)
                .recipientRole(RecipientRole.ADMIN)
                .isRead(false)
                .isArchived(false)
                .build();
        notifAdmin = notificationRepository.save(notifAdmin);

        // 2. Create VETERINARIAN Notification
        notifVet = Notification.builder()
                .title("Vet Alert")
                .message("Vaccination schedule is ready.")
                .notificationType(NotificationType.HEALTH)
                .severity(Severity.WARNING)
                .sourceModule(SourceModule.HEALTH)
                .recipientRole(RecipientRole.VETERINARIAN)
                .isRead(false)
                .isArchived(false)
                .build();
        notifVet = notificationRepository.save(notifVet);

        // 3. Create ALL Notification
        notifAll = Notification.builder()
                .title("General Notice")
                .message("Farming updates for today.")
                .notificationType(NotificationType.GENERAL)
                .severity(Severity.INFO)
                .sourceModule(SourceModule.SYSTEM)
                .recipientRole(RecipientRole.ALL)
                .isRead(false)
                .isArchived(false)
                .build();
        notifAll = notificationRepository.save(notifAll);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchNotifications_asAdmin_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2))) // should see ADMIN and ALL, but not VETERINARIAN
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("Admin Alert", "General Notice")))
                .andExpect(jsonPath("$.data.content[*].title", not(hasItem("Vet Alert"))));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testSearchNotifications_asVet_Success() throws Exception {
        mockMvc.perform(get("/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .param("type", "HEALTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Vet Alert")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testGetNotificationById_asVet_Success() throws Exception {
        mockMvc.perform(get("/notifications/" + notifVet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Vet Alert")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testGetNotificationById_Forbidden_ForOtherRole() throws Exception {
        // Veterinarian should not access Admin notifications
        mockMvc.perform(get("/notifications/" + notifAdmin.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testMarkAsRead_Success() throws Exception {
        mockMvc.perform(patch("/notifications/" + notifVet.getId() + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.read", is(true)));

        Notification updated = notificationRepository.findById(notifVet.getId()).orElseThrow();
        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testMarkAsRead_Forbidden_ForOtherRole() throws Exception {
        mockMvc.perform(patch("/notifications/" + notifAdmin.getId() + "/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testArchiveNotification_Success() throws Exception {
        mockMvc.perform(patch("/notifications/" + notifVet.getId() + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.archived", is(true)));

        Notification updated = notificationRepository.findById(notifVet.getId()).orElseThrow();
        assertTrue(updated.isArchived());
        assertNotNull(updated.getArchivedAt());
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testModifyArchived_ThrowsValidationException() throws Exception {
        // Archive it first
        notifVet.setArchived(true);
        notifVet.setArchivedAt(LocalDateTime.now());
        notificationRepository.save(notifVet);

        // Try marking read
        mockMvc.perform(patch("/notifications/" + notifVet.getId() + "/read"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Archived notifications cannot be modified")));

        // Try archiving again
        mockMvc.perform(patch("/notifications/" + notifVet.getId() + "/archive"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Archived notifications cannot be modified")));
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testBulkRead_Success() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(2))); // Vet has notifVet and notifAll unread

        Notification updatedVet = notificationRepository.findById(notifVet.getId()).orElseThrow();
        Notification updatedAll = notificationRepository.findById(notifAll.getId()).orElseThrow();
        assertTrue(updatedVet.isRead());
        assertTrue(updatedAll.isRead());
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testGetUnreadCount_Success() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(2))); // notifVet and notifAll

        // Mark one as read
        notifVet.setRead(true);
        notifVet.setReadAt(LocalDateTime.now());
        notificationRepository.save(notifVet);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1))); // only notifAll left
    }

    @Test
    @WithMockUser(roles = "VETERINARIAN")
    void testGetUnreadCount_ExcludesArchived() throws Exception {
        // Archive the unread Vet notification
        notifVet.setArchived(true);
        notifVet.setArchivedAt(LocalDateTime.now());
        notificationRepository.save(notifVet);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1))); // only notifAll is active & unread
    }
}
