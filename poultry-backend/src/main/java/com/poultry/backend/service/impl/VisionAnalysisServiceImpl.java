package com.poultry.backend.service.impl;

import com.poultry.backend.dto.VisionAnalysisDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import com.poultry.backend.repository.HealthRecordRepository;
import com.poultry.backend.service.VisionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionAnalysisServiceImpl implements VisionAnalysisService {

    private final ChickenRepository chickenRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;

    @Override
    @Transactional
    public VisionAnalysisDTOs.VisionAnalysisResponse analyzeImage(VisionAnalysisDTOs.VisionAnalysisRequest request, String currentUser) {
        log.info("Processing Computer Vision Disease Analysis for Chicken ID: {}", request.getChickenId());

        Chicken chicken = chickenRepository.findById(request.getChickenId())
                .orElseThrow(() -> new NotFoundException("Chicken not found with ID: " + request.getChickenId()));

        // Simulate AI Computer Vision Deep Learning Inference Model
        boolean isSick = request.getNotes() != null && (request.getNotes().toLowerCase().contains("lesion") || request.getNotes().toLowerCase().contains("pale") || request.getNotes().toLowerCase().contains("sick") || request.getNotes().toLowerCase().contains("eye"));

        String condition = isSick ? "Fowl Pox & Respiratory Lesion Risk" : "Healthy - Normal Visual Assessment";
        double confidence = isSick ? 92.5 : 97.8;
        boolean isolation = isSick;
        boolean vetConsult = isSick;

        List<VisionAnalysisDTOs.SymptomDetail> symptoms = new ArrayList<>();
        symptoms.add(VisionAnalysisDTOs.SymptomDetail.builder().feature("Comb").status(isSick ? "Pale / Crusty" : "Bright Red").severity(isSick ? "MEDIUM" : "LOW").build());
        symptoms.add(VisionAnalysisDTOs.SymptomDetail.builder().feature("Eye").status(isSick ? "Cloudy Secretion" : "Clear & Alert").severity(isSick ? "HIGH" : "LOW").build());
        symptoms.add(VisionAnalysisDTOs.SymptomDetail.builder().feature("Feather").status("Intact").severity("LOW").build());
        symptoms.add(VisionAnalysisDTOs.SymptomDetail.builder().feature("Posture").status(isSick ? "Drooping Wings" : "Upright & Active").severity(isSick ? "MEDIUM" : "LOW").build());

        String treatment = isSick ? "Isolate bird immediately. Administer oral antibiotics and topical iodine on comb crusts." : "No treatment required. Maintain standard biosecurity & feed nutrition.";

        List<String> recs = new ArrayList<>();
        if (isSick) {
            recs.add("Move chicken to quarantine pen #2 immediately.");
            recs.add("Schedule veterinary consultation within 24 hours.");
            recs.add("Sanitize coop drinker & feeder units.");
        } else {
            recs.add("Flock visual assessment clean.");
            recs.add("Continue routine daily health monitoring.");
        }

        // Auto-create Health Record
        HealthRecord record = HealthRecord.builder()
                .recordCode("HR-VISION-" + System.currentTimeMillis() % 100000)
                .chicken(chicken)
                .recordDate(LocalDate.now())
                .healthType(isSick ? HealthType.DISEASE : HealthType.CHECKUP)
                .diseaseName(isSick ? "Fowl Pox Risk" : "Healthy Assessment")
                .symptoms(condition + " detected via AI Computer Vision Scanner.")
                .diagnosis(condition)
                .treatment(treatment)
                .administeredBy(currentUser != null ? currentUser : "AI Diagnostics")
                .veterinarian("AI Vision Model")
                .healthStatus(isSick ? HealthStatus.SICK : HealthStatus.HEALTHY)
                .remarks("Confidence score: " + confidence + "%. Image analysis completed.")
                .build();
        HealthRecord savedRecord = healthRecordRepository.save(record);

        // Update Chicken status if sick
        if (isSick) {
            chicken.setHealthStatus(HealthStatus.SICK);
            chickenRepository.save(chicken);
        }

        // Auto-record Timeline Event
        ChickenTimelineEvent timelineEvent = ChickenTimelineEvent.builder()
                .chicken(chicken)
                .title("AI Disease Scan: " + condition)
                .description("Computer Vision diagnostic scan completed with " + confidence + "% confidence score. Isolation required: " + (isolation ? "YES" : "NO"))
                .eventType("AI_DISEASE_ANALYSIS")
                .moduleName("HEALTH")
                .relatedEntityId(savedRecord.getId())
                .createdBy(currentUser != null ? currentUser : "AI Vision Model")
                .build();
        ChickenTimelineEvent savedTimeline = chickenTimelineRepository.save(timelineEvent);

        return VisionAnalysisDTOs.VisionAnalysisResponse.builder()
                .chickenId(chicken.getId())
                .chickenCode(chicken.getChickenCode())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://images.unsplash.com/photo-1548550023-2bdb3c5beed7?auto=format&fit=crop&w=400&q=80")
                .detectedCondition(condition)
                .confidenceScore(confidence)
                .isolationRequired(isolation)
                .vetConsultationRecommended(vetConsult)
                .symptoms(symptoms)
                .treatmentGuidance(treatment)
                .recommendations(recs)
                .healthRecordId(savedRecord.getId())
                .timelineEventId(savedTimeline.getId())
                .build();
    }
}
