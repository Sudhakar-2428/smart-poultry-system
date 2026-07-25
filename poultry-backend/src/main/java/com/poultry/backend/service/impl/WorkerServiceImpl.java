package com.poultry.backend.service.impl;

import com.poultry.backend.dto.WorkerRequest;
import com.poultry.backend.dto.WorkerResponse;
import com.poultry.backend.dto.WorkerUpdateRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

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
                .role(Role.WORKER)
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
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                Optional<User> existingPhoneUser = userRepository.findByEmail(targetUser.getEmail());
                // Double check if phone is taken by a different user ID
                boolean existsOther = userRepository.findAll().stream()
                        .anyMatch(u -> !u.getId().equals(targetUser.getId()) && request.getPhoneNumber().equals(u.getPhoneNumber()));
                if (existsOther) {
                    throw new DuplicateRecordException("Phone number is already registered by another user: " + request.getPhoneNumber());
                }
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
        // Try finding by FarmMember primary key first
        Optional<FarmMember> byMemberId = farmMemberRepository.findById(workerId);
        if (byMemberId.isPresent() && byMemberId.get().getFarm().getId().equals(farmId)) {
            return byMemberId.get();
        }

        // Try finding by FarmId and User ID
        Optional<FarmMember> byUserId = farmMemberRepository.findByFarmIdAndUserId(farmId, workerId);
        if (byUserId.isPresent()) {
            return byUserId.get();
        }

        throw new NotFoundException("Worker not found with ID: " + workerId + " in farm ID: " + farmId);
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
