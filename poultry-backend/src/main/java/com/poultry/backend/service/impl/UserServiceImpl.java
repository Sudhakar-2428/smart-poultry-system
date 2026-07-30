package com.poultry.backend.service.impl;

import com.poultry.backend.dto.ChangePasswordRequest;
import com.poultry.backend.dto.UserDto;
import com.poultry.backend.dto.UserUpdateRequest;
import com.poultry.backend.entity.User;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.UnauthorizedException;
import com.poultry.backend.mapper.UserMapper;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.MembershipStatus;
import com.poultry.backend.repository.FarmMemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FarmMemberRepository farmMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        log.info("Fetching profile details for user email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        UserDto dto = userMapper.toDto(user);

        List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            FarmMember activeMember = memberships.stream()
                    .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                    .findFirst()
                    .orElse(memberships.get(0));
            if (activeMember != null && activeMember.getRole() != null) {
                dto.setCurrentFarmRole(activeMember.getRole().name());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Processing password change request for user email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed due to invalid current password verification for email: {}", email);
            throw new UnauthorizedException("Invalid current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("AUDIT: Password change successfully processed for user: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.info("Fetching list of all registered users");
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.info("Fetching profile details for user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating details for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        // Validate email uniqueness if email has changed
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateRecordException("Email '" + request.getEmail() + "' is already registered.");
            }
            user.setEmail(request.getEmail());
        }

        // Validate phone uniqueness if phone has changed
        if (!user.getPhoneNumber().equalsIgnoreCase(request.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new DuplicateRecordException("Phone number '" + request.getPhoneNumber() + "' is already registered.");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user.setFullName(request.getFullName());
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        log.info("User details successfully updated for user ID: {}", id);

        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto updateUserStatus(Long id, Boolean active) {
        log.info("Updating active status for user ID: {} to {}", id, active);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        user.setActive(active);
        User updatedUser = userRepository.save(user);

        if (Boolean.TRUE.equals(active)) {
            log.info("AUDIT: User activation processed for user ID: {}", id);
        } else {
            log.info("AUDIT: User deactivation processed for user ID: {}", id);
        }

        return userMapper.toDto(updatedUser);
    }
}
