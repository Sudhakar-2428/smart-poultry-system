package com.poultry.backend.service.impl;

import com.poultry.backend.dto.FarmMemberResponse;
import com.poultry.backend.dto.JoinRequest;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.service.FarmMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmMemberServiceImpl implements FarmMemberService {

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FarmMemberResponse createJoinRequest(JoinRequest request, String currentUserEmail) {
        log.info("User {} is requesting to join farm with code {} as {}", currentUserEmail, request.getJoinCode(), request.getRole());

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Validate Join Code
        Farm farm = farmRepository.findByJoinCode(request.getJoinCode())
                .orElseThrow(() -> new ValidationException("Invalid farm join code."));

        // Validate Requested Role
        if (request.getRole() == FarmRole.PRIMARY_OWNER) {
            throw new ValidationException("Cannot request to join as flags PRIMARY_OWNER.");
        }

        // Validate duplicate membership
        Optional<FarmMember> existingMemberOpt = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId());
        if (existingMemberOpt.isPresent()) {
            FarmMember existing = existingMemberOpt.get();
            if (existing.getStatus() == MembershipStatus.APPROVED) {
                throw new DuplicateRecordException("You are already an approved member of this farm.");
            } else if (existing.getStatus() == MembershipStatus.PENDING) {
                throw new DuplicateRecordException("You already have a pending join request for this farm.");
            } else {
                // If they were REMOVED or REJECTED, reset to PENDING and update role
                existing.setStatus(MembershipStatus.PENDING);
                existing.setRole(request.getRole());
                FarmMember updated = farmMemberRepository.save(existing);
                log.info("Resetting membership status to PENDING for user: {} in farm: {}", currentUserEmail, farm.getId());
                return mapToResponse(updated);
            }
        }

        // Create new join request
        FarmMember newMember = FarmMember.builder()
                .farm(farm)
                .user(user)
                .role(request.getRole())
                .status(MembershipStatus.PENDING)
                .build();
        newMember = farmMemberRepository.save(newMember);

        log.info("AUDIT: Access join request created. Member ID: {}, User: {}, Farm ID: {}", 
                newMember.getId(), currentUserEmail, farm.getId());

        return mapToResponse(newMember);
    }

    @Override
    @Transactional
    public FarmMemberResponse approveMember(Long memberId, String currentUserEmail) {
        log.info("Approving member ID: {} by manager/owner {}", memberId, currentUserEmail);

        FarmMember targetMember = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Farm member not found with ID: " + memberId));

        if (targetMember.getStatus() != MembershipStatus.PENDING) {
            throw new ValidationException("Only PENDING members can be approved.");
        }

        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(targetMember.getFarm().getId(), caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership is not approved.");
        }

        // Check security rules
        if (callerMembership.getRole() == FarmRole.PRIMARY_OWNER) {
            // Primary owner can approve anyone
        } else if (callerMembership.getRole() == FarmRole.CO_OWNER) {
            // Co-Owner can only approve workers
            if (targetMember.getRole() != FarmRole.WORKER) {
                throw new AccessDeniedException("Co-Owners can only approve/reject WORKER level requests.");
            }
        } else {
            // Managers, Workers, Family Members have no approval rights
            throw new AccessDeniedException("You do not have administrative privileges to approve members.");
        }

        targetMember.setStatus(MembershipStatus.APPROVED);
        FarmMember approved = farmMemberRepository.save(targetMember);
        log.info("AUDIT: Member approved. Member ID: {}, Farm ID: {}, Role: {}", approved.getId(), approved.getFarm().getId(), approved.getRole());

        return mapToResponse(approved);
    }

    @Override
    @Transactional
    public FarmMemberResponse rejectMember(Long memberId, String currentUserEmail) {
        log.info("Rejecting member ID: {} by manager/owner {}", memberId, currentUserEmail);

        FarmMember targetMember = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Farm member not found with ID: " + memberId));

        if (targetMember.getStatus() != MembershipStatus.PENDING) {
            throw new ValidationException("Only PENDING members can be rejected.");
        }

        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(targetMember.getFarm().getId(), caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership is not approved.");
        }

        // Check security rules
        if (callerMembership.getRole() == FarmRole.PRIMARY_OWNER) {
            // Primary owner can reject anyone
        } else if (callerMembership.getRole() == FarmRole.CO_OWNER) {
            // Co-Owner can only reject workers
            if (targetMember.getRole() != FarmRole.WORKER) {
                throw new AccessDeniedException("Co-Owners can only approve/reject WORKER level requests.");
            }
        } else {
            // Managers, Workers, Family Members have no approval rights
            throw new AccessDeniedException("You do not have administrative privileges to reject members.");
        }

        targetMember.setStatus(MembershipStatus.REJECTED);
        FarmMember rejected = farmMemberRepository.save(targetMember);
        log.info("AUDIT: Member rejected. Member ID: {}, Farm ID: {}", rejected.getId(), rejected.getFarm().getId());

        return mapToResponse(rejected);
    }

    @Override
    @Transactional
    public void removeMember(Long memberId, String currentUserEmail) {
        log.info("Removing member ID: {} by manager/owner {}", memberId, currentUserEmail);

        FarmMember targetMember = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Farm member not found with ID: " + memberId));

        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(targetMember.getFarm().getId(), caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership is not approved.");
        }

        // Check security rules
        if (callerMembership.getRole() == FarmRole.PRIMARY_OWNER) {
            // Primary owner can remove anyone except themselves if they are the last Primary Owner
            if (targetMember.getId().equals(callerMembership.getId())) {
                throw new ValidationException("Cannot remove yourself if you are the Primary Owner. Transfer ownership first.");
            }
        } else if (callerMembership.getRole() == FarmRole.CO_OWNER) {
            // Co-Owner can only remove workers, and cannot remove managers, co-owners or primary owners
            if (targetMember.getRole() != FarmRole.WORKER) {
                throw new AccessDeniedException("Co-Owners can only remove WORKER level members.");
            }
        } else {
            throw new AccessDeniedException("You do not have administrative privileges to remove members.");
        }

        targetMember.setStatus(MembershipStatus.REMOVED);
        farmMemberRepository.save(targetMember);
        log.info("AUDIT: Member removed. Member ID: {}, Farm ID: {}", memberId, targetMember.getFarm().getId());
    }

    @Override
    @Transactional
    public FarmMemberResponse changeMemberRole(Long memberId, FarmRole newRole, String currentUserEmail) {
        log.info("Changing role of member ID: {} to {} by {}", memberId, newRole, currentUserEmail);

        FarmMember targetMember = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Farm member not found with ID: " + memberId));

        if (targetMember.getStatus() != MembershipStatus.APPROVED) {
            throw new ValidationException("Can only change role for APPROVED members.");
        }

        User caller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Caller not found with email: " + currentUserEmail));

        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(targetMember.getFarm().getId(), caller.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership is not approved.");
        }

        // Only PRIMARY_OWNER can change roles
        if (callerMembership.getRole() != FarmRole.PRIMARY_OWNER) {
            throw new AccessDeniedException("Only the Primary Owner can change member roles.");
        }

        // If target is already Primary Owner, and role changes, check validation
        if (targetMember.getRole() == FarmRole.PRIMARY_OWNER && newRole != FarmRole.PRIMARY_OWNER) {
            throw new ValidationException("Cannot remove Primary Owner role unless transferring ownership to someone else first.");
        }

        // If newRole is PRIMARY_OWNER, transfer ownership (demote current primary owner to CO_OWNER)
        if (newRole == FarmRole.PRIMARY_OWNER) {
            log.info("Transferring Primary Ownership of Farm {} from {} to {}", 
                    targetMember.getFarm().getId(), caller.getEmail(), targetMember.getUser().getEmail());
            callerMembership.setRole(FarmRole.CO_OWNER);
            farmMemberRepository.save(callerMembership);
        }

        targetMember.setRole(newRole);
        FarmMember updated = farmMemberRepository.save(targetMember);
        log.info("AUDIT: Member role updated. Member ID: {}, New Role: {}", updated.getId(), updated.getRole());

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResponse> getPendingRequests(String currentUserEmail) {
        log.info("Fetching pending join requests for owner/manager: {}", currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Find all memberships of this user
        List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());

        // Get farm IDs where caller is Primary Owner or Co-Owner, and membership is approved
        List<Long> manageableFarmIds = memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .filter(m -> m.getRole() == FarmRole.PRIMARY_OWNER || m.getRole() == FarmRole.CO_OWNER)
                .map(m -> m.getFarm().getId())
                .collect(Collectors.toList());

        // Get all pending requests for those farms
        return manageableFarmIds.stream()
                .flatMap(farmId -> farmMemberRepository.findByFarmIdAndStatus(farmId, MembershipStatus.PENDING).stream())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResponse> getFarmMembers(String farmUniqueId, String currentUserEmail) {
        log.info("Fetching members list for Farm: {} by User: {}", farmUniqueId, currentUserEmail);

        Farm farm = farmRepository.findByFarmUniqueId(farmUniqueId)
                .orElseThrow(() -> new NotFoundException("Farm not found with Unique ID: " + farmUniqueId));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + currentUserEmail));

        // Verify caller belongs to this farm
        FarmMember callerMembership = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this farm."));

        if (callerMembership.getStatus() != MembershipStatus.APPROVED) {
            throw new AccessDeniedException("Your membership is not approved.");
        }

        // Get all approved members of this farm
        List<FarmMember> members = farmMemberRepository.findByFarmId(farm.getId());

        return members.stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FarmMemberResponse mapToResponse(FarmMember member) {
        return FarmMemberResponse.builder()
                .id(member.getId())
                .farmId(member.getFarm().getId())
                .farmName(member.getFarm().getName())
                .userId(member.getUser().getId())
                .userFullName(member.getUser().getFullName())
                .userEmail(member.getUser().getEmail())
                .role(member.getRole())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
