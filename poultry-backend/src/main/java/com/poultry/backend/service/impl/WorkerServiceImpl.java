package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.UnauthorizedException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.UserMapper;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.NotificationRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.service.WorkerService;
import com.poultry.backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getWorkers(Long farmId, String currentUserEmail) {
        log.info("Fetching workers for farm ID: {} by user: {}", farmId, currentUserEmail);
        Farm farm = getFarmOrThrow(farmId);
        validateOwnerOrCoOwnerAccess(farm.getId(), currentUserEmail);

        List<FarmMember> members = farmMemberRepository.findByFarmId(farm.getId());
        return members.stream()
                .filter(m -> m.getStatus() != MembershipStatus.REMOVED)
                .map(this::mapToWorkerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkerResponse createWorker(Long farmId, WorkerRequest request, String currentUserEmail) {
        log.info("Creating worker for farm ID: {} by caller: {}", farmId, currentUserEmail);
        Farm farm = getFarmOrThrow(farmId);
        validateOwnerOrCoOwnerAccess(farm.getId(), currentUserEmail);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecordException("Email is already registered: " + request.getEmail());
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateRecordException("Phone number is already registered: " + request.getPhoneNumber());
        }

        FarmRole targetRole = request.getRole() != null ? request.getRole() : FarmRole.WORKER;

        User workerUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        workerUser = userRepository.save(workerUser);

        FarmMember farmMember = FarmMember.builder()
                .farm(farm)
                .user(workerUser)
                .role(targetRole)
                .status(MembershipStatus.APPROVED)
                .build();
        farmMember = farmMemberRepository.save(farmMember);

        log.info("Successfully created worker account. User ID: {}, Member ID: {}, Farm ID: {}",
                workerUser.getId(), farmMember.getId(), farm.getId());

        return mapToWorkerResponse(farmMember);
    }

    @Override
    @Transactional
    public WorkerInviteResponse inviteWorker(Long farmId, WorkerInviteRequest request, String currentUserEmail) {
        log.info("Inviting worker for farm ID: {} by caller: {}", farmId, currentUserEmail);
        Farm farm = getFarmOrThrow(farmId);
        validateOwnerOrCoOwnerAccess(farm.getId(), currentUserEmail);

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhoneNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateRecordException("Email is already registered: " + email);
        }

        if (userRepository.existsByPhoneNumber(phone)) {
            throw new DuplicateRecordException("Phone number is already registered: " + phone);
        }

        String tempPassword = generateRandomTempPassword();
        FarmRole targetRole = request.getRole() != null ? request.getRole() : FarmRole.WORKER;

        User workerUser = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .phoneNumber(phone)
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .mustChangePassword(true)
                .build();
        workerUser = userRepository.save(workerUser);

        FarmMember farmMember = FarmMember.builder()
                .farm(farm)
                .user(workerUser)
                .role(targetRole)
                .status(MembershipStatus.PENDING)
                .build();
        farmMember = farmMemberRepository.save(farmMember);

        String formattedWorkerId = "WRK-" + workerUser.getId();

        log.info("Successfully invited worker. User ID: {}, Formatted ID: {}, Member ID: {}, Farm ID: {}",
                workerUser.getId(), formattedWorkerId, farmMember.getId(), farm.getId());

        return WorkerInviteResponse.builder()
                .id(farmMember.getId())
                .userId(workerUser.getId())
                .workerId(formattedWorkerId)
                .farmId(farm.getId())
                .farmUniqueId(farm.getFarmUniqueId())
                .fullName(workerUser.getFullName())
                .email(workerUser.getEmail())
                .phoneNumber(workerUser.getPhoneNumber())
                .temporaryPassword(tempPassword)
                .role(farmMember.getRole())
                .status(farmMember.getStatus())
                .createdAt(farmMember.getCreatedAt() != null ? farmMember.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse joinFarmWithTempPassword(JoinFarmTempRequest request) {
        log.info("Worker attempting to join farm ID/UniqueId: {} with Worker ID/Email: {}",
                request.getFarmId(), request.getWorkerId());

        Farm farm = resolveFarm(request.getFarmId());
        User workerUser = resolveWorkerUser(request.getWorkerId());

        FarmMember member = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), workerUser.getId())
                .orElseThrow(() -> new NotFoundException("No pending invitation found for worker in this farm."));

        if (!passwordEncoder.matches(request.getTemporaryPassword(), workerUser.getPassword())) {
            log.warn("Join farm failed - Invalid temporary password for worker: {}", workerUser.getEmail());
            throw new UnauthorizedException("Invalid temporary password or invitation expired.");
        }

        if (member.getStatus() == MembershipStatus.APPROVED) {
            log.info("Worker email {} has already joined farm ID {}.", workerUser.getEmail(), farm.getId());
        } else {
            member.setStatus(MembershipStatus.APPROVED);
            farmMemberRepository.save(member);
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            workerUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        workerUser.setMustChangePassword(false);
        userRepository.save(workerUser);

        // Send notifications
        // 1. To Farm Primary Owner(s)
        List<FarmMember> owners = farmMemberRepository.findByFarmIdAndRole(farm.getId(), FarmRole.PRIMARY_OWNER);
        for (FarmMember ownerMember : owners) {
            Notification ownerNotif = Notification.builder()
                    .title("Worker Joined Successfully")
                    .message(workerUser.getFullName() + " has successfully joined your farm " + farm.getName() + ".")
                    .notificationType(NotificationType.SYSTEM)
                    .severity(Severity.INFO)
                    .sourceModule(SourceModule.SYSTEM)
                    .recipientRole(RecipientRole.ADMIN)
                    .targetId(workerUser.getId())
                    .build();
            notificationRepository.save(ownerNotif);
        }

        // 2. To Worker
        Notification workerNotif = Notification.builder()
                .title("Welcome to " + farm.getName())
                .message("Welcome to " + farm.getName() + "! Your worker account is now active.")
                .notificationType(NotificationType.SYSTEM)
                .severity(Severity.INFO)
                .sourceModule(SourceModule.SYSTEM)
                .recipientRole(RecipientRole.WORKER)
                .targetId(workerUser.getId())
                .build();
        notificationRepository.save(workerNotif);

        CustomUserDetails userDetails = new CustomUserDetails(workerUser);
        String token = jwtUtils.generateToken(userDetails);

        log.info("Worker successfully joined farm. User ID: {}, Farm ID: {}", workerUser.getId(), farm.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationMs())
                .user(userMapper.toDto(workerUser))
                .build();
    }

    @Override
    @Transactional
    public WorkerResponse updateWorker(Long farmId, Long workerId, WorkerUpdateRequest request, String currentUserEmail) {
        log.info("Updating worker ID: {} in farm ID: {} by caller: {}", workerId, farmId, currentUserEmail);
        Farm farm = getFarmOrThrow(farmId);
        validateOwnerOrCoOwnerAccess(farm.getId(), currentUserEmail);

        FarmMember targetMember = findTargetMemberOrThrow(farm.getId(), workerId);
        User targetUser = targetMember.getUser();

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(targetUser.getEmail())) {
            Optional<User> existingEmailUser = userRepository.findByEmail(request.getEmail());
            if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(targetUser.getId())) {
                throw new DuplicateRecordException("Email is already registered by another user: " + request.getEmail());
            }
            targetUser.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(targetUser.getPhoneNumber())) {
            boolean existsOther = userRepository.findAll().stream()
                    .anyMatch(u -> !u.getId().equals(targetUser.getId()) && request.getPhoneNumber().equals(u.getPhoneNumber()));
            if (existsOther) {
                throw new DuplicateRecordException("Phone number is already registered by another user: " + request.getPhoneNumber());
            }
            targetUser.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            targetUser.setFullName(request.getFullName());
        }

        userRepository.save(targetUser);

        if (request.getRole() != null) {
            targetMember.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            targetMember.setStatus(request.getStatus());
        }

        FarmMember updatedMember = farmMemberRepository.save(targetMember);
        log.info("Successfully updated worker ID: {}", workerId);

        return mapToWorkerResponse(updatedMember);
    }

    @Override
    @Transactional
    public void deleteWorker(Long farmId, Long workerId, String currentUserEmail) {
        log.info("Deleting worker ID: {} from farm ID: {} by caller: {}", workerId, farmId, currentUserEmail);
        Farm farm = getFarmOrThrow(farmId);
        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller user not found with email: " + currentUserEmail));
        validateOwnerOrCoOwnerAccess(farm.getId(), currentUserEmail);

        FarmMember targetMember = findTargetMemberOrThrow(farm.getId(), workerId);
        User targetUser = targetMember.getUser();

        if (targetMember.getRole() == FarmRole.PRIMARY_OWNER) {
            throw new ValidationException("Cannot delete Farm Owner.");
        }

        if (targetUser.getId().equals(caller.getId())) {
            throw new ValidationException("Cannot delete yourself.");
        }

        farmMemberRepository.delete(targetMember);
        farmMemberRepository.flush();

        List<FarmMember> remainingMemberships = farmMemberRepository.findByUserId(targetUser.getId());
        if (remainingMemberships.isEmpty()) {
            try {
                userRepository.delete(targetUser);
                userRepository.flush();
                log.info("Deleted orphan user account for worker ID: {}", targetUser.getId());
            } catch (Exception e) {
                log.warn("Could not delete user entity due to references, marking inactive: {}", e.getMessage());
                targetUser.setActive(false);
                userRepository.save(targetUser);
            }
        }

        log.info("Successfully deleted worker membership ID: {} from farm ID: {}", workerId, farmId);
    }

    private Farm getFarmOrThrow(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID: " + farmId));
    }

    private void validateOwnerOrCoOwnerAccess(Long farmId, String currentUserEmail) {
        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller user not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(farmId, caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership status is not approved.");
        }

        if (callerMembership.getRole() != FarmRole.PRIMARY_OWNER && callerMembership.getRole() != FarmRole.CO_OWNER) {
            throw new AccessDeniedException("Workers are not authorized to manage farm workers.");
        }
    }

    private FarmMember findTargetMemberOrThrow(Long farmId, Long workerId) {
        Optional<FarmMember> byMemberId = farmMemberRepository.findById(workerId);
        if (byMemberId.isPresent() && byMemberId.get().getFarm().getId().equals(farmId)) {
            return byMemberId.get();
        }

        Optional<FarmMember> byUserId = farmMemberRepository.findByFarmIdAndUserId(farmId, workerId);
        if (byUserId.isPresent()) {
            return byUserId.get();
        }

        throw new NotFoundException("Worker not found with ID: " + workerId + " in farm ID: " + farmId);
    }

    private Farm resolveFarm(String farmIdInput) {
        if (farmIdInput == null || farmIdInput.isBlank()) {
            throw new ValidationException("Farm ID is required.");
        }
        String clean = farmIdInput.trim();
        try {
            Long numericId = Long.parseLong(clean);
            Optional<Farm> byId = farmRepository.findById(numericId);
            if (byId.isPresent()) return byId.get();
        } catch (NumberFormatException ignored) {}

        return farmRepository.findByFarmUniqueId(clean)
                .orElseThrow(() -> new NotFoundException("Farm not found with ID or Unique ID: " + farmIdInput));
    }

    private User resolveWorkerUser(String workerIdInput) {
        if (workerIdInput == null || workerIdInput.isBlank()) {
            throw new ValidationException("Worker ID or Email is required.");
        }
        String clean = workerIdInput.trim();
        if (clean.toUpperCase().startsWith("WRK-")) {
            clean = clean.substring(4);
        }

        try {
            Long numericId = Long.parseLong(clean);
            Optional<User> byId = userRepository.findById(numericId);
            if (byId.isPresent()) return byId.get();
        } catch (NumberFormatException ignored) {}

        return userRepository.findByEmail(clean.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Worker not found with ID or Email: " + workerIdInput));
    }

    private String generateRandomTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private WorkerResponse mapToWorkerResponse(FarmMember member) {
        return WorkerResponse.builder()
                .id(member.getId())
                .workerId(member.getUser().getId())
                .farmId(member.getFarm().getId())
                .fullName(member.getUser().getFullName())
                .email(member.getUser().getEmail())
                .phoneNumber(member.getUser().getPhoneNumber())
                .role(member.getRole())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt() != null ? member.getCreatedAt() : member.getUser().getCreatedAt())
                .updatedAt(member.getUpdatedAt() != null ? member.getUpdatedAt() : member.getUser().getUpdatedAt())
                .build();
    }
}
