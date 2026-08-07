package com.poultry.backend.service.impl;

import com.poultry.backend.dto.ChickenTimelineDTOs;
import com.poultry.backend.entity.Chicken;
import com.poultry.backend.entity.ChickenTimelineEvent;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import com.poultry.backend.service.ChickenTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChickenTimelineServiceImpl implements ChickenTimelineService {

    private final ChickenTimelineRepository chickenTimelineRepository;
    private final ChickenRepository chickenRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChickenTimelineDTOs.TimelineEventDTO> getChickenTimeline(Long chickenId, String eventType, String moduleName, String startDate, String endDate, String search) {
        log.info("Retrieving timeline events for chicken ID: {}", chickenId);

        Chicken chicken = chickenRepository.findById(chickenId)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + chickenId));

        List<ChickenTimelineEvent> events = chickenTimelineRepository.findByChickenIdOrderByTimestampDesc(chicken.getId());

        List<ChickenTimelineDTOs.TimelineEventDTO> filtered = events.stream()
                .filter(e -> filterEvent(e, eventType, moduleName, startDate, endDate, search))
                .map(this::toEventDTO)
                .toList();

        log.info("Found {} timeline events for chicken ID: {}", filtered.size(), chickenId);
        return filtered;
    }

    @Override
    @Transactional
    public ChickenTimelineDTOs.TimelineEventDTO addManualNote(Long chickenId, ChickenTimelineDTOs.CreateTimelineNoteRequest request, String currentUser) {
        log.info("Adding manual timeline note for chicken ID: {}", chickenId);

        Chicken chicken = chickenRepository.findById(chickenId)
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + chickenId));

        ChickenTimelineEvent event = ChickenTimelineEvent.builder()
                .chicken(chicken)
                .eventType("NOTE")
                .title(request.getTitle())
                .description(request.getDescription())
                .moduleName(request.getModuleName() != null ? request.getModuleName() : "PROFILE")
                .relatedEntityId(request.getRelatedEntityId())
                .createdBy(currentUser != null ? currentUser : "User")
                .build();

        ChickenTimelineEvent saved = chickenTimelineRepository.save(event);
        return toEventDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ChickenTimelineDTOs.TimelineReportDTO getTimelineReport(String eventType, String moduleName, String startDate, String endDate, String search) {
        log.info("Generating global timeline report");

        List<ChickenTimelineEvent> events = chickenTimelineRepository.findAllByOrderByTimestampDesc();

        List<ChickenTimelineDTOs.TimelineEventDTO> filtered = events.stream()
                .filter(e -> filterEvent(e, eventType, moduleName, startDate, endDate, search))
                .map(this::toEventDTO)
                .toList();

        return ChickenTimelineDTOs.TimelineReportDTO.builder()
                .reportTitle("Chicken Lifecycle Timeline Report")
                .totalEvents((long) filtered.size())
                .events(filtered)
                .build();
    }

    private boolean filterEvent(ChickenTimelineEvent e, String eventType, String moduleName, String startDate, String endDate, String search) {
        if (eventType != null && !eventType.trim().isEmpty() && !"ALL".equalsIgnoreCase(eventType)) {
            if (e.getEventType() == null || !e.getEventType().equalsIgnoreCase(eventType.trim())) {
                return false;
            }
        }

        if (moduleName != null && !moduleName.trim().isEmpty() && !"ALL".equalsIgnoreCase(moduleName)) {
            if (e.getModuleName() == null || !e.getModuleName().equalsIgnoreCase(moduleName.trim())) {
                return false;
            }
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                LocalDate start = LocalDate.parse(startDate.trim());
                if (e.getTimestamp() != null && e.getTimestamp().toLocalDate().isBefore(start)) {
                    return false;
                }
            } catch (Exception ignored) {}
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                LocalDate end = LocalDate.parse(endDate.trim());
                if (e.getTimestamp() != null && e.getTimestamp().toLocalDate().isAfter(end)) {
                    return false;
                }
            } catch (Exception ignored) {}
        }

        if (search != null && !search.trim().isEmpty()) {
            String kw = search.toLowerCase().trim();
            boolean matchTitle = e.getTitle() != null && e.getTitle().toLowerCase().contains(kw);
            boolean matchDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(kw);
            boolean matchType = e.getEventType() != null && e.getEventType().toLowerCase().contains(kw);
            boolean matchUser = e.getCreatedBy() != null && e.getCreatedBy().toLowerCase().contains(kw);
            boolean matchChicken = e.getChicken() != null && e.getChicken().getChickenCode().toLowerCase().contains(kw);
            if (!matchTitle && !matchDesc && !matchType && !matchUser && !matchChicken) {
                return false;
            }
        }

        return true;
    }

    private ChickenTimelineDTOs.TimelineEventDTO toEventDTO(ChickenTimelineEvent e) {
        String module = e.getModuleName();
        if (module == null || module.trim().isEmpty()) {
            String type = e.getEventType() != null ? e.getEventType().toUpperCase() : "";
            if (type.contains("PAIR")) module = "PAIRING";
            else if (type.contains("EGG")) module = "EGG_COLLECTION";
            else if (type.contains("HATCH") || type.contains("CANDLING") || type.contains("INCUBAT")) module = "HATCHING";
            else if (type.contains("HEALTH") || type.contains("VACCIN")) module = "HEALTH";
            else if (type.contains("SALE")) module = "SALES";
            else module = "CHICKEN_PROFILE";
        }

        String navLink = "/flock.html?id=" + (e.getChicken() != null ? e.getChicken().getId() : "");
        if ("PAIRING".equalsIgnoreCase(module)) navLink = "/pairing.html";
        else if ("EGG_COLLECTION".equalsIgnoreCase(module)) navLink = "/egg-collection.html";
        else if ("HATCHING".equalsIgnoreCase(module)) navLink = "/hatching.html";
        else if ("HEALTH".equalsIgnoreCase(module)) navLink = "/health-records.html";
        else if ("SALES".equalsIgnoreCase(module)) navLink = "/sales.html";

        return ChickenTimelineDTOs.TimelineEventDTO.builder()
                .id(e.getId())
                .chickenId(e.getChicken() != null ? e.getChicken().getId() : null)
                .chickenCode(e.getChicken() != null ? e.getChicken().getChickenCode() : null)
                .chickenName(e.getChicken() != null ? e.getChicken().getName() : null)
                .eventType(e.getEventType())
                .title(e.getTitle())
                .description(e.getDescription())
                .createdBy(e.getCreatedBy() != null ? e.getCreatedBy() : "System")
                .moduleName(module)
                .relatedEntityId(e.getRelatedEntityId())
                .moduleNavigationLink(navLink)
                .timestamp(e.getTimestamp() != null ? e.getTimestamp() : LocalDateTime.now())
                .build();
    }
}
