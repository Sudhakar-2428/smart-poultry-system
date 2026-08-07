package com.poultry.backend.service.impl;

import com.poultry.backend.dto.PurchaseBatchDTOs;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.ChickenRepository;
import com.poultry.backend.repository.ChickenTimelineRepository;
import com.poultry.backend.repository.PurchaseBatchRepository;
import com.poultry.backend.service.PurchaseBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseBatchServiceImpl implements PurchaseBatchService {

    private final PurchaseBatchRepository purchaseBatchRepository;
    private final ChickenRepository chickenRepository;
    private final ChickenTimelineRepository chickenTimelineRepository;

    @Override
    @Transactional
    public PurchaseBatchDTOs.PurchaseBatchResponse createPurchaseBatch(PurchaseBatchDTOs.CreatePurchaseBatchRequest request) {
        log.info("Creating Purchase Batch from Supplier: {}", request.getSupplierName());

        String batchCode = request.getBatchCode();
        if (batchCode == null || batchCode.trim().isEmpty()) {
            long nextId = purchaseBatchRepository.getMaxPurchaseBatchId() + 1;
            batchCode = String.format("PB%02d", nextId);
        }

        PurchaseBatch batch = PurchaseBatch.builder()
                .batchCode(batchCode)
                .supplierName(request.getSupplierName())
                .supplierContact(request.getSupplierContact())
                .purchaseDate(request.getPurchaseDate())
                .invoiceNumber(request.getInvoiceNumber())
                .purchaseCost(request.getPurchaseCost())
                .transportCost(request.getTransportCost())
                .totalChickensCount(request.getTotalChickensCount())
                .remarks(request.getRemarks())
                .build();

        PurchaseBatch savedBatch = purchaseBatchRepository.save(batch);

        Double unitCost = (request.getPurchaseCost() != null && request.getTotalChickensCount() > 0)
                ? (request.getPurchaseCost().doubleValue() / request.getTotalChickensCount())
                : null;

        List<PurchaseBatchDTOs.PurchasedChickenDTO> registeredChickens = new ArrayList<>();

        for (int i = 1; i <= request.getTotalChickensCount(); i++) {
            String seqStr = String.format("%03d", i);
            String intelligentCode = batchCode + "-" + seqStr;

            int attempt = 1;
            while (chickenRepository.existsByChickenCode(intelligentCode)) {
                intelligentCode = batchCode + "-" + String.format("%03d", i + (attempt * 100));
                attempt++;
            }

            Chicken chicken = Chicken.builder()
                    .chickenCode(intelligentCode)
                    .category(request.getCategory() != null ? request.getCategory() : ChickenCategory.COUNTRY_CHICKEN)
                    .breed(request.getBreed() != null ? request.getBreed() : Breed.COUNTRY_CHICKEN)
                    .gender(request.getGender() != null ? request.getGender() : Gender.FEMALE)
                    .origin(ChickenOrigin.PURCHASED)
                    .purchaseDate(request.getPurchaseDate())
                    .purchaseCost(unitCost)
                    .supplierName(request.getSupplierName())
                    .supplierContact(request.getSupplierContact())
                    .purchaseBatchId(savedBatch.getId())
                    .dateOfBirth(request.getPurchaseDate().minusDays(180))
                    .healthStatus(HealthStatus.HEALTHY)
                    .status(ChickenStatus.ACTIVE)
                    .weight(request.getAverageWeight())
                    .remarks("Registered under Purchase Batch " + batchCode)
                    .build();

            Chicken savedChicken = chickenRepository.save(chicken);

            String qrUrl = "/flock.html?id=" + savedChicken.getId();

            // Timeline Event 1: Purchased
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(savedChicken)
                    .title("Purchased")
                    .description("Purchased from supplier " + request.getSupplierName() + " in Batch " + batchCode)
                    .eventType("PURCHASED")
                    .createdBy("System")
                    .build());

            // Timeline Event 2: Registered
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(savedChicken)
                    .title("Registered")
                    .description("Registered with Purchased Code " + intelligentCode)
                    .eventType("REGISTERED")
                    .createdBy("System")
                    .build());

            // Timeline Event 3: QR Generated
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(savedChicken)
                    .title("QR Generated")
                    .description("QR Code generated linking to chicken profile: " + qrUrl)
                    .eventType("QR_GENERATED")
                    .createdBy("System")
                    .build());

            // Timeline Event 4: Added To Farm
            chickenTimelineRepository.save(ChickenTimelineEvent.builder()
                    .chicken(savedChicken)
                    .title("Added To Farm")
                    .description("Added to active farm flock inventory.")
                    .eventType("ADDED_TO_FARM")
                    .createdBy("System")
                    .build());

            registeredChickens.add(PurchaseBatchDTOs.PurchasedChickenDTO.builder()
                    .id(savedChicken.getId())
                    .chickenCode(savedChicken.getChickenCode())
                    .name(savedChicken.getName())
                    .breed(savedChicken.getBreed() != null ? savedChicken.getBreed().name() : "COUNTRY_CHICKEN")
                    .category(savedChicken.getCategory() != null ? savedChicken.getCategory().name() : "COUNTRY_CHICKEN")
                    .gender(savedChicken.getGender() != null ? savedChicken.getGender().name() : "FEMALE")
                    .origin(savedChicken.getOrigin() != null ? savedChicken.getOrigin().name() : "PURCHASED")
                    .purchaseDate(savedChicken.getPurchaseDate())
                    .purchaseCost(savedChicken.getPurchaseCost())
                    .supplierName(savedChicken.getSupplierName())
                    .supplierContact(savedChicken.getSupplierContact())
                    .purchaseBatchCode(batchCode)
                    .qrCodeUrl(qrUrl)
                    .build());
        }

        log.info("Successfully registered {} purchased chickens for Batch Code: {}", request.getTotalChickensCount(), batchCode);

        return PurchaseBatchDTOs.PurchaseBatchResponse.builder()
                .id(savedBatch.getId())
                .batchCode(savedBatch.getBatchCode())
                .supplierName(savedBatch.getSupplierName())
                .supplierContact(savedBatch.getSupplierContact())
                .purchaseDate(savedBatch.getPurchaseDate())
                .invoiceNumber(savedBatch.getInvoiceNumber())
                .purchaseCost(savedBatch.getPurchaseCost())
                .transportCost(savedBatch.getTransportCost())
                .totalChickensCount(savedBatch.getTotalChickensCount())
                .remarks(savedBatch.getRemarks())
                .registeredChickens(registeredChickens)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseBatchDTOs.PurchaseBatchResponse> getAllPurchaseBatches() {
        return purchaseBatchRepository.findAll().stream()
                .map(this::toBatchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseBatchDTOs.PurchaseBatchResponse getPurchaseBatchById(Long id) {
        PurchaseBatch batch = purchaseBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase batch not found with ID: " + id));
        return toBatchResponse(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseBatchDTOs.PurchasedChickenReportDTO getPurchasedChickenReport(String reportType, String supplierName) {
        List<Chicken> purchasedChickens = chickenRepository.findAll().stream()
                .filter(c -> c.getOrigin() == ChickenOrigin.PURCHASED)
                .toList();

        if (supplierName != null && !supplierName.trim().isEmpty()) {
            String lowerSupp = supplierName.toLowerCase().trim();
            purchasedChickens = purchasedChickens.stream()
                    .filter(c -> c.getSupplierName() != null && c.getSupplierName().toLowerCase().contains(lowerSupp))
                    .toList();
        }

        List<PurchaseBatchDTOs.PurchasedChickenDTO> dtoList = purchasedChickens.stream().map(c -> {
            String batchCode = "PB01";
            if (c.getPurchaseBatchId() != null) {
                batchCode = purchaseBatchRepository.findById(c.getPurchaseBatchId())
                        .map(PurchaseBatch::getBatchCode)
                        .orElse("PB01");
            }
            return PurchaseBatchDTOs.PurchasedChickenDTO.builder()
                    .id(c.getId())
                    .chickenCode(c.getChickenCode())
                    .name(c.getName())
                    .breed(c.getBreed() != null ? c.getBreed().name() : "COUNTRY_CHICKEN")
                    .category(c.getCategory() != null ? c.getCategory().name() : "COUNTRY_CHICKEN")
                    .gender(c.getGender() != null ? c.getGender().name() : "FEMALE")
                    .origin(c.getOrigin() != null ? c.getOrigin().name() : "PURCHASED")
                    .purchaseDate(c.getPurchaseDate())
                    .purchaseCost(c.getPurchaseCost())
                    .supplierName(c.getSupplierName())
                    .supplierContact(c.getSupplierContact())
                    .purchaseBatchCode(batchCode)
                    .qrCodeUrl("/flock.html?id=" + c.getId())
                    .build();
        }).toList();

        long totalBatches = purchaseBatchRepository.count();
        double totalCost = dtoList.stream()
                .mapToDouble(c -> c.getPurchaseCost() != null ? c.getPurchaseCost() : 0.0)
                .sum();

        return PurchaseBatchDTOs.PurchasedChickenReportDTO.builder()
                .reportTitle((reportType != null ? reportType.toUpperCase() : "PURCHASED") + " Chicken Report")
                .totalBatches(totalBatches)
                .totalChickens((long) dtoList.size())
                .totalSpend(BigDecimal.valueOf(totalCost))
                .chickens(dtoList)
                .build();
    }

    private PurchaseBatchDTOs.PurchaseBatchResponse toBatchResponse(PurchaseBatch batch) {
        List<Chicken> chickens = chickenRepository.findAll().stream()
                .filter(c -> c.getPurchaseBatchId() != null && c.getPurchaseBatchId().equals(batch.getId()))
                .toList();

        List<PurchaseBatchDTOs.PurchasedChickenDTO> registered = chickens.stream().map(c ->
                PurchaseBatchDTOs.PurchasedChickenDTO.builder()
                        .id(c.getId())
                        .chickenCode(c.getChickenCode())
                        .name(c.getName())
                        .breed(c.getBreed() != null ? c.getBreed().name() : "COUNTRY_CHICKEN")
                        .category(c.getCategory() != null ? c.getCategory().name() : "COUNTRY_CHICKEN")
                        .gender(c.getGender() != null ? c.getGender().name() : "FEMALE")
                        .origin(c.getOrigin() != null ? c.getOrigin().name() : "PURCHASED")
                        .purchaseDate(c.getPurchaseDate())
                        .purchaseCost(c.getPurchaseCost())
                        .supplierName(c.getSupplierName())
                        .supplierContact(c.getSupplierContact())
                        .purchaseBatchCode(batch.getBatchCode())
                        .qrCodeUrl("/flock.html?id=" + c.getId())
                        .build()
        ).toList();

        return PurchaseBatchDTOs.PurchaseBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .supplierName(batch.getSupplierName())
                .supplierContact(batch.getSupplierContact())
                .purchaseDate(batch.getPurchaseDate())
                .invoiceNumber(batch.getInvoiceNumber())
                .purchaseCost(batch.getPurchaseCost())
                .transportCost(batch.getTransportCost())
                .totalChickensCount(batch.getTotalChickensCount())
                .remarks(batch.getRemarks())
                .registeredChickens(registered)
                .build();
    }
}
