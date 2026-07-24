package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.UnauthorizedException;
import com.poultry.backend.mapper.UserMapper;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.service.AuthService;
import com.poultry.backend.util.JwtUtils;
import com.poultry.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    private final com.poultry.backend.repository.FarmRepository farmRepository;
    private final com.poultry.backend.repository.FarmMemberRepository farmMemberRepository;

    @Value("${app.jwt.prefix:Bearer }")
    private String jwtPrefix;

    @Override
    @Transactional
    public UserDto register(RegisterRequest registerRequest) {
        log.info("Processing user registration attempt for email: {}", registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateRecordException("Email '" + registerRequest.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new DuplicateRecordException("Phone number '" + registerRequest.getPhoneNumber() + "' is already registered.");
        }

        // Default role is WORKER, unless requested by an ADMIN
        Role assignedRole = Role.WORKER;
        if (registerRequest.getRole() != null) {
            if (SecurityUtils.hasRole("ADMIN")) {
                assignedRole = registerRequest.getRole();
                log.info("Admin caller overriding role assignment to: {}", assignedRole);
            } else {
                log.warn("Non-admin caller tried to assign role: {}. Defaults to WORKER.", registerRequest.getRole());
            }
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(assignedRole)
                .isActive(true)
                .emailVerified(true) // baseline register endpoint assumes verified to keep backwards compatibility
                .build();

        User savedUser = userRepository.save(user);
        log.info("AUDIT: User registration processed for email: {}, assigned role: {}", savedUser.getEmail(), savedUser.getRole());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto registerOwner(OwnerRegisterRequest request) {
        log.info("Processing owner registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecordException("Email '" + request.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateRecordException("Phone number '" + request.getPhoneNumber() + "' is already registered.");
        }

        String verificationToken = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.MANAGER) // system role is MANAGER for owners
                .isActive(true)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .pendingFarmName(request.getFarmName())
                .pendingFarmAddress(request.getFarmAddress())
                .pendingLatitude(request.getLatitude())
                .pendingLongitude(request.getLongitude())
                .build();

        User savedUser = userRepository.save(user);
        log.info("AUDIT: Owner registered. ID: {}, verification token: {}", savedUser.getId(), verificationToken);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto registerWorker(WorkerRegisterRequest request) {
        log.info("Processing worker/family member registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecordException("Email '" + request.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateRecordException("Phone number '" + request.getPhoneNumber() + "' is already registered.");
        }

        String verificationToken = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.WORKER) // system role is WORKER
                .isActive(true)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .pendingFarmName(null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("AUDIT: Worker registered. ID: {}, verification token: {}", savedUser.getId(), verificationToken);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        log.info("Verifying email for: {} with token: {}", request.getEmail(), request.getToken());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.poultry.backend.exception.NotFoundException("User not found with email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            log.info("Email already verified for: {}", request.getEmail());
            return;
        }

        if (user.getEmailVerificationToken() == null || !user.getEmailVerificationToken().equals(request.getToken())) {
            throw new com.poultry.backend.exception.ValidationException("Invalid email verification token.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        // If Owner: Automatically create a Farm and generate details
        if (user.getPendingFarmName() != null) {
            log.info("Owner email verified post registration. Creating farm: {}", user.getPendingFarmName());
            
            // Create Farm
            Farm farm = Farm.builder()
                    .name(user.getPendingFarmName())
                    .farmAddress(user.getPendingFarmAddress())
                    .latitude(user.getPendingLatitude())
                    .longitude(user.getPendingLongitude())
                    .locationLastUpdated(user.getPendingLatitude() != null && user.getPendingLongitude() != null ? java.time.LocalDateTime.now() : null)
                    .build();
            // PrePersist hook auto generates farmUniqueId & joinCode
            farm = farmRepository.save(farm);

            // Create FarmMember entry with role PRIMARY_OWNER and APPROVED
            FarmMember membership = FarmMember.builder()
                    .farm(farm)
                    .user(user)
                    .role(FarmRole.PRIMARY_OWNER)
                    .status(MembershipStatus.APPROVED)
                    .build();
            farmMemberRepository.save(membership);

            user.setPendingFarmName(null); // clear after creation
            user.setPendingFarmAddress(null);
            user.setPendingLatitude(null);
            user.setPendingLongitude(null);
            userRepository.save(user);

            log.info("Automatically generated Farm ID: {}, Unique ID: {}, Join Code: {} for Owner: {}",
                    farm.getId(), farm.getFarmUniqueId(), farm.getJoinCode(), user.getEmail());
        }

        log.info("AUDIT: Email successfully verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void joinFarm(JoinFarmRequest request) {
        log.info("Enrolling user email: {} to join farm {} with join code {}", 
                request.getEmail(), request.getFarmUniqueId(), request.getJoinCode());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.poultry.backend.exception.NotFoundException("User not found with email: " + request.getEmail()));

        // Verification: Must verify email before Join Request is created
        if (!user.isEmailVerified()) {
            throw new com.poultry.backend.exception.ValidationException("Please verify your email before joining a farm.");
        }

        // Validate unique ID and Join Code
        Farm farm = farmRepository.findByFarmUniqueId(request.getFarmUniqueId())
                .orElseThrow(() -> new com.poultry.backend.exception.ValidationException("Invalid Farm Unique ID."));

        if (!farm.getJoinCode().equals(request.getJoinCode())) {
            throw new com.poultry.backend.exception.ValidationException("Invalid Farm Join Code.");
        }

        // Validate Role selected: WORKER or FAMILY_MEMBER
        if (request.getRole() != FarmRole.WORKER && request.getRole() != FarmRole.FAMILY_MEMBER) {
            throw new com.poultry.backend.exception.ValidationException("Role must be either WORKER or FAMILY_MEMBER.");
        }

        // Validate duplicate membership
        java.util.Optional<FarmMember> existing = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId());
        if (existing.isPresent()) {
            if (existing.get().getStatus() == MembershipStatus.APPROVED) {
                throw new DuplicateRecordException("You are already an approved member of this farm.");
            } else if (existing.get().getStatus() == MembershipStatus.PENDING) {
                throw new DuplicateRecordException("Duplicate Join Request: You already have a pending request.");
            } else {
                // If rejected or removed, let them re-request
                FarmMember member = existing.get();
                member.setStatus(MembershipStatus.PENDING);
                member.setRole(request.getRole());
                farmMemberRepository.save(member);
                log.info("Re-submitted join request for: {} in farm: {}", user.getEmail(), farm.getId());
                return;
            }
        }

        // Create FarmMember entry (PENDING status)
        FarmMember membership = FarmMember.builder()
                .farm(farm)
                .user(user)
                .role(request.getRole())
                .status(MembershipStatus.PENDING)
                .build();
        farmMemberRepository.save(membership);

        log.info("AUDIT: Pending join request created for User: {} in Farm ID: {}", user.getEmail(), farm.getId());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Processing user login attempt for email: {}", loginRequest.getEmail());

        // Validate email verification and active status first
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password."));

        if (!user.isEmailVerified()) {
            log.warn("Login attempt blocked for unverified email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Email is not verified. Please verify your email first.");
        }

        if (!user.isActive()) {
            log.warn("Inactive login attempt blocked for email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("User account is inactive. Please contact the administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Update lastLogin timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtils.generateToken(userDetails);
            log.info("AUDIT: User login successful for email: {}", loginRequest.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .tokenType(jwtPrefix.trim())
                    .expiresIn(jwtUtils.getExpirationMs())
                    .user(userMapper.toDto(user))
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed credentials login attempt for email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Invalid username or password.");
        } catch (DisabledException e) {
            log.warn("Disabled user account login attempt for email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("User account is disabled.");
        }
    }

}
