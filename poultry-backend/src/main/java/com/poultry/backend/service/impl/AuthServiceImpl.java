package com.poultry.backend.service.impl;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.UnauthorizedException;
import com.poultry.backend.mapper.UserMapper;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
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

    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;

    @Value("${app.jwt.prefix:Bearer }")
    private String jwtPrefix;

    @Override
    @Transactional
    public UserDto register(RegisterRequest registerRequest) {
        if (registerRequest.getEmail() != null) {
            registerRequest.setEmail(registerRequest.getEmail().trim().toLowerCase());
        }
        if (registerRequest.getPhoneNumber() != null) {
            registerRequest.setPhoneNumber(registerRequest.getPhoneNumber().trim());
        }
        log.info("Processing user registration attempt for email: {}", registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration rejected - duplicate email: {}", registerRequest.getEmail());
            throw new DuplicateRecordException("Email '" + registerRequest.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            log.warn("Registration rejected - duplicate phone number: {}", registerRequest.getPhoneNumber());
            throw new DuplicateRecordException("Phone number '" + registerRequest.getPhoneNumber() + "' is already registered.");
        }

        // System role defaults to USER
        Role assignedRole = Role.USER;
        if (registerRequest.getRole() != null) {
            if (SecurityUtils.hasRole("SUPER_ADMIN")) {
                assignedRole = registerRequest.getRole();
                log.info("Super admin caller overriding role assignment to: {}", assignedRole);
            } else {
                log.warn("Non-admin caller tried to assign role: {}. Defaults to USER.", registerRequest.getRole());
            }
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(assignedRole)
                .isActive(true)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("AUDIT: User registration processed for email: {}, assigned role: {}", savedUser.getEmail(), savedUser.getRole());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto registerOwner(OwnerRegisterRequest request) {
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhoneNumber() != null) {
            request.setPhoneNumber(request.getPhoneNumber().trim());
        }
        log.info("Processing owner registration attempt for email: {}, farm: {}", request.getEmail(), request.getFarmName());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Owner registration rejected - duplicate email: {}", request.getEmail());
            throw new DuplicateRecordException("Email '" + request.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Owner registration rejected - duplicate phone number: {}", request.getPhoneNumber());
            throw new DuplicateRecordException("Phone number '" + request.getPhoneNumber() + "' is already registered.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // system role is USER for farm owners
                .isActive(true)
                .emailVerified(true) // Auto-verify owner upon registration
                .build();

        User savedUser = userRepository.save(user);

        // Automatically create Farm & Primary Owner membership upon registration
        if (request.getFarmName() != null && !request.getFarmName().isBlank()) {
            Farm farm = Farm.builder()
                    .name(request.getFarmName())
                    .farmAddress(request.getFarmAddress())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .locationLastUpdated(request.getLatitude() != null && request.getLongitude() != null ? LocalDateTime.now() : null)
                    .build();
            // PrePersist hook auto generates farmUniqueId & joinCode
            farm = farmRepository.save(farm);

            FarmMember membership = FarmMember.builder()
                    .farm(farm)
                    .user(savedUser)
                    .role(FarmRole.PRIMARY_OWNER)
                    .status(MembershipStatus.APPROVED)
                    .build();
            farmMemberRepository.save(membership);

            log.info("AUDIT: Farm successfully created for owner. Farm ID: {}, Unique ID: {}, Join Code: {}",
                    farm.getId(), farm.getFarmUniqueId(), farm.getJoinCode());
        }

        log.info("AUDIT: Owner registered and activated. User ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto registerWorker(WorkerRegisterRequest request) {
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhoneNumber() != null) {
            request.setPhoneNumber(request.getPhoneNumber().trim());
        }
        log.info("Processing worker/family member registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Worker registration rejected - duplicate email: {}", request.getEmail());
            throw new DuplicateRecordException("Email '" + request.getEmail() + "' is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Worker registration rejected - duplicate phone number: {}", request.getPhoneNumber());
            throw new DuplicateRecordException("Phone number '" + request.getPhoneNumber() + "' is already registered.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // system role is USER for workers
                .isActive(true)
                .emailVerified(true) // Auto-verify worker upon registration
                .build();

        User savedUser = userRepository.save(user);
        log.info("AUDIT: Worker registered and activated. User ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());

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

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("AUDIT: Email successfully verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void joinFarm(JoinFarmRequest request) {
        log.info("Enrolling user email: {} to join farm {} with join code {}", 
                request.getEmail(), request.getFarmUniqueId(), request.getJoinCode());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.poultry.backend.exception.NotFoundException("User not found with email: " + request.getEmail()));

        // Validate unique ID and Join Code
        Farm farm = farmRepository.findByFarmUniqueId(request.getFarmUniqueId())
                .orElseThrow(() -> new com.poultry.backend.exception.ValidationException("Invalid Farm Unique ID."));

        if (!farm.getJoinCode().equals(request.getJoinCode())) {
            throw new com.poultry.backend.exception.ValidationException("Invalid Farm Join Code.");
        }

        // Validate Role selected
        if (request.getRole() == null) {
            throw new com.poultry.backend.exception.ValidationException("Role must be specified.");
        }

        // Validate duplicate membership
        java.util.Optional<FarmMember> existing = farmMemberRepository.findByFarmIdAndUserId(farm.getId(), user.getId());
        if (existing.isPresent()) {
            if (existing.get().getStatus() == MembershipStatus.APPROVED) {
                throw new DuplicateRecordException("You are already an approved member of this farm.");
            } else if (existing.get().getStatus() == MembershipStatus.PENDING) {
                throw new DuplicateRecordException("Duplicate Join Request: You already have a pending request.");
            } else {
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
        if (loginRequest.getEmail() != null) {
            loginRequest.setEmail(loginRequest.getEmail().trim().toLowerCase());
        }
        log.info("Processing user login attempt for email: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user email not found: {}", loginRequest.getEmail());
                    return new UnauthorizedException("Invalid username or password.");
                });

        // Ensure email is verified for backwards compatibility with any existing test accounts
        if (!user.isEmailVerified()) {
            log.info("Auto-verifying user email during login attempt for: {}", loginRequest.getEmail());
            user.setEmailVerified(true);
            userRepository.save(user);
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

            String systemRole = user.getRole() != null ? user.getRole().name() : Role.USER.name();

            // Determine active farm role from farmMemberRepository as the authoritative farm role
            String currentFarmRole = null;
            java.util.List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());
            if (!memberships.isEmpty()) {
                FarmMember activeMember = memberships.stream()
                        .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                        .findFirst()
                        .orElse(memberships.get(0));
                if (activeMember != null && activeMember.getRole() != null) {
                    currentFarmRole = activeMember.getRole().name();
                }
            }

            UserDto userDto = userMapper.toDto(user);
            userDto.setSystemRole(systemRole);
            userDto.setCurrentFarmRole(currentFarmRole);

            return AuthResponse.builder()
                    .token(token)
                    .tokenType(jwtPrefix.trim())
                    .expiresIn(jwtUtils.getExpirationMs())
                    .user(userDto)
                    .systemRole(systemRole)
                    .currentFarmRole(currentFarmRole)
                    .build();

        } catch (BadCredentialsException | org.springframework.security.authentication.InternalAuthenticationServiceException e) {
            log.warn("Failed credentials login attempt for email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Invalid username or password.");
        } catch (DisabledException e) {
            log.warn("Disabled user account login attempt for email: {}", loginRequest.getEmail());
            throw new UnauthorizedException("User account is disabled.");
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Authentication exception for email {}: {}", loginRequest.getEmail(), e.getMessage());
            throw new UnauthorizedException("Invalid username or password.");
        }
    }
}
