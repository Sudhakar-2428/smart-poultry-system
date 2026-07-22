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

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final WeatherService weatherService;

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
}
