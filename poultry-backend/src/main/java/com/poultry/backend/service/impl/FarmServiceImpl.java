package com.poultry.backend.service.impl;

import com.poultry.backend.dto.FarmLocationUpdateRequest;
import com.poultry.backend.dto.FarmRequest;
import com.poultry.backend.dto.FarmResponse;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.service.FarmService;
import com.poultry.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.poultry.backend.dto.*;
import com.poultry.backend.repository.ChickenRepository;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final ChickenRepository chickenRepository;

    @Override
    @Transactional
    public FarmResponse createFarm(FarmRequest request, String currentUserEmail) {
        log.info("Creating a new farm: {} for creator email: {}", request.getName(), currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Create Farm with coordinates if provided
        Farm farm = Farm.builder()
                .name(request.getName())
                .farmAddress(request.getFarmAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationLastUpdated(request.getLatitude() != null && request.getLongitude() != null ? LocalDateTime.now() : null)
                .build();
        farm = farmRepository.save(farm);

        // Add creator as Primary Owner
        FarmMember membership = FarmMember.builder()
                .farm(farm)
                .user(user)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMemberRepository.save(membership);

        log.info("AUDIT: Farm successfully created. Farm ID: {}, Unique ID: {}, Join Code: {}, Lat: {}, Lon: {}", 
                farm.getId(), farm.getFarmUniqueId(), farm.getJoinCode(), farm.getLatitude(), farm.getLongitude());

        return mapToResponse(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long id) {
        log.info("Fetching farm details for ID: {}", id);
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + id));
        return mapToResponse(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmByUniqueId(String farmUniqueId) {
        log.info("Fetching farm details for Unique ID: {}", farmUniqueId);
        Farm farm = farmRepository.findByFarmUniqueId(farmUniqueId)
                .orElseThrow(() -> new NotFoundException("Farm not found with Unique ID: " + farmUniqueId));
        return mapToResponse(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmResponse> getMyFarms(String currentUserEmail) {
        log.info("Fetching all active farm memberships for user email: {}", currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());

        return memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(m -> mapToResponse(m.getFarm()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String regenerateJoinCode(String farmUniqueId, String currentUserEmail) {
        log.info("Attempting to regenerate join code for Farm: {} by User: {}", farmUniqueId, currentUserEmail);

        Farm farm = farmRepository.findByFarmUniqueId(farmUniqueId)
                .orElseThrow(() -> new NotFoundException("Farm not found with Unique ID: " + farmUniqueId));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Security check: Only PRIMARY_OWNER can regenerate join code
        FarmMember currentMembership = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm"));

        if (currentMembership.getRole() != FarmRole.PRIMARY_OWNER) {
            log.warn("Access Denied: Non-owner {} tried to regenerate join code for Farm: {}", currentUserEmail, farmUniqueId);
            throw new AccessDeniedException("Only the Primary Owner can regenerate the join code");
        }

        // Regenerate and save
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String newCode = sb.toString();
        farm.setJoinCode(newCode);
        farmRepository.save(farm);

        log.info("AUDIT: Join code regenerated successfully. Farm ID: {}, New Join Code: {}", farm.getId(), newCode);
        return newCode;
    }

    @Override
    @Transactional
    public FarmResponse updateFarmLocation(Long farmId, FarmLocationUpdateRequest request, String currentUserEmail) {
        log.info("Updating location coordinates for Farm ID: {} by User: {}", farmId, currentUserEmail);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Security Check: Only Primary Owner or Co-Owner (Manager) can update farm location
        FarmMember currentMembership = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm"));

        if (currentMembership.getRole() != FarmRole.PRIMARY_OWNER && currentMembership.getRole() != FarmRole.CO_OWNER) {
            log.warn("Access Denied: User {} with role {} tried to update location for Farm: {}", currentUserEmail, currentMembership.getRole(), farmId);
            throw new AccessDeniedException("Only the Farm Owner or Manager can update farm location coordinates");
        }

        farm.setFarmAddress(request.getFarmAddress());
        farm.setLatitude(request.getLatitude());
        farm.setLongitude(request.getLongitude());
        farm.setLocationLastUpdated(LocalDateTime.now());

        farm = farmRepository.save(farm);

        // Immediately refresh cached weather for this farm
        try {
            weatherService.refreshFarmWeather(farmId);
            log.info("Refreshed weather for Farm ID: {} following location update", farmId);
        } catch (Exception e) {
            log.warn("Non-blocking exception refreshing weather post location update for Farm ID {}: {}", farmId, e.getMessage());
        }

        log.info("AUDIT: Farm location successfully updated for Farm ID: {}. Lat: {}, Lon: {}", farmId, farm.getLatitude(), farm.getLongitude());
        return mapToResponse(farm);
    }

    private FarmResponse mapToResponse(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .farmUniqueId(farm.getFarmUniqueId())
                .joinCode(farm.getJoinCode())
                .farmAddress(farm.getFarmAddress())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .locationLastUpdated(farm.getLocationLastUpdated())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FarmProfileResponse getFarmProfile(Long farmId, String currentUserEmail) {
        log.info("Fetching farm profile for farm ID: {} by user: {}", farmId, currentUserEmail);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        List<FarmMember> members = farmMemberRepository.findByFarmId(farm.getId());
        String ownerName = members.stream()
                .filter(m -> m.getRole() == FarmRole.PRIMARY_OWNER)
                .map(m -> m.getUser().getFullName())
                .findFirst()
                .orElseGet(() -> {
                    User caller = userRepository.findByEmail(currentUserEmail).orElse(null);
                    return caller != null ? caller.getFullName() : "Primary Owner";
                });

        long totalWorkers = members.stream()
                .filter(m -> m.getStatus() != MembershipStatus.REMOVED && m.getRole() != FarmRole.PRIMARY_OWNER)
                .count();

        long totalChickens = chickenRepository.count();

        return FarmProfileResponse.builder()
                .farmId(farm.getId())
                .farmUniqueId(farm.getFarmUniqueId())
                .farmName(farm.getName())
                .logoUrl(farm.getLogoUrl())
                .ownerName(ownerName)
                .email(farm.getEmail())
                .phone(farm.getPhone())
                .farmAddress(farm.getFarmAddress())
                .village(farm.getVillage())
                .district(farm.getDistrict())
                .state(farm.getState())
                .country(farm.getCountry())
                .pinCode(farm.getPinCode())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .totalWorkers(totalWorkers)
                .totalChickens(totalChickens)
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public FarmProfileResponse updateFarmProfile(Long farmId, FarmProfileUpdateRequest request, String currentUserEmail) {
        log.info("Updating farm profile for farm ID: {} by user: {}", farmId, currentUserEmail);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        validatePrimaryOwnerAccess(farm.getId(), currentUserEmail);

        if (request.getFarmName() != null && !request.getFarmName().isBlank()) {
            farm.setName(request.getFarmName().trim());
        }
        if (request.getEmail() != null) {
            farm.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            farm.setPhone(request.getPhone().trim());
        }
        if (request.getFarmAddress() != null) {
            farm.setFarmAddress(request.getFarmAddress().trim());
        }
        if (request.getVillage() != null) {
            farm.setVillage(request.getVillage().trim());
        }
        if (request.getDistrict() != null) {
            farm.setDistrict(request.getDistrict().trim());
        }
        if (request.getState() != null) {
            farm.setState(request.getState().trim());
        }
        if (request.getCountry() != null) {
            farm.setCountry(request.getCountry().trim());
        }
        if (request.getPinCode() != null) {
            farm.setPinCode(request.getPinCode().trim());
        }
        if (request.getLatitude() != null) {
            farm.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            farm.setLongitude(request.getLongitude());
        }
        farm.setLocationLastUpdated(LocalDateTime.now());

        farm = farmRepository.save(farm);
        log.info("Successfully updated farm profile for farm ID: {}", farmId);

        return getFarmProfile(farmId, currentUserEmail);
    }

    @Override
    @Transactional
    public FarmProfileResponse uploadFarmLogo(Long farmId, MultipartFile file, String currentUserEmail) {
        log.info("Uploading farm logo for farm ID: {} by user: {}", farmId, currentUserEmail);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        validatePrimaryOwnerAccess(farm.getId(), currentUserEmail);

        if (file == null || file.isEmpty()) {
            throw new com.poultry.backend.exception.ValidationException("File cannot be empty.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new com.poultry.backend.exception.ValidationException("Logo file size cannot exceed 5 MB.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        boolean isValidExt = filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".webp");
        boolean isValidMime = contentType.contains("image/jpeg") || contentType.contains("image/jpg") || contentType.contains("image/png") || contentType.contains("image/webp");

        if (!isValidExt && !isValidMime) {
            throw new com.poultry.backend.exception.ValidationException("Invalid file format. Allowed formats: JPG, JPEG, PNG, WEBP.");
        }

        try {
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            String mime = contentType.isBlank() ? "image/png" : contentType;
            String dataUrl = "data:" + mime + ";base64," + base64;
            farm.setLogoUrl(dataUrl);
            farmRepository.save(farm);
            log.info("Successfully uploaded farm logo for farm ID: {}", farmId);
        } catch (Exception e) {
            log.error("Failed to read logo file bytes", e);
            throw new com.poultry.backend.exception.ValidationException("Could not process uploaded image file.");
        }

        return getFarmProfile(farmId, currentUserEmail);
    }

    @Override
    @Transactional
    public FarmProfileResponse deleteFarmLogo(Long farmId, String currentUserEmail) {
        log.info("Deleting farm logo for farm ID: {} by user: {}", farmId, currentUserEmail);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));

        validatePrimaryOwnerAccess(farm.getId(), currentUserEmail);

        farm.setLogoUrl(null);
        farmRepository.save(farm);
        log.info("Successfully deleted farm logo for farm ID: {}", farmId);

        return getFarmProfile(farmId, currentUserEmail);
    }

    private void validatePrimaryOwnerAccess(Long farmId, String currentUserEmail) {
        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(farmId, caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership status is not approved.");
        }

        if (callerMembership.getRole() != FarmRole.PRIMARY_OWNER) {
            throw new AccessDeniedException("Only the Primary Farm Owner can edit farm profile details.");
        }
    }
}
