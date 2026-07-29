package com.poultry.backend.service;

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
import com.poultry.backend.service.impl.WorkerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkerServiceImplTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FarmMemberRepository farmMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WorkerServiceImpl workerService;

    private Farm farm;
    private User ownerUser;
    private FarmMember ownerMember;
    private User workerUser;
    private FarmMember workerMember;

    @BeforeEach
    void setUp() {
        farm = Farm.builder()
                .id(1L)
                .name("Sunshine Farm")
                .farmUniqueId("FARM-100")
                .joinCode("CODE1234")
                .build();

        ownerUser = User.builder()
                .id(10L)
                .fullName("Owner Bob")
                .email("owner.bob@example.com")
                .phoneNumber("+1111111111")
                .role(Role.USER)
                .password("encoded")
                .isActive(true)
                .build();

        ownerMember = FarmMember.builder()
                .id(100L)
                .farm(farm)
                .user(ownerUser)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();

        workerUser = User.builder()
                .id(20L)
                .fullName("Worker Alice")
                .email("alice@example.com")
                .phoneNumber("+2222222222")
                .role(Role.USER)
                .password("encoded")
                .isActive(true)
                .build();

        workerMember = FarmMember.builder()
                .id(200L)
                .farm(farm)
                .user(workerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();
    }

    @Test
    void getWorkers_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(farmMemberRepository.findByFarmId(1L)).thenReturn(List.of(ownerMember, workerMember));

        List<WorkerResponse> result = workerService.getWorkers(1L, ownerUser.getEmail());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Worker Alice", result.get(1).getFullName());
    }

    @Test
    void getWorkers_UnauthorizedForWorkerRole() {
        User regularWorkerUser = User.builder().id(30L).email("worker.user@example.com").build();
        FarmMember regularWorkerMember = FarmMember.builder()
                .id(300L)
                .farm(farm)
                .user(regularWorkerUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(regularWorkerUser.getEmail())).thenReturn(Optional.of(regularWorkerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 30L)).thenReturn(Optional.of(regularWorkerMember));

        assertThrows(AccessDeniedException.class, () ->
                workerService.getWorkers(1L, regularWorkerUser.getEmail())
        );
    }

    @Test
    void createWorker_Success() {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("New Worker")
                .email("new.worker@example.com")
                .phoneNumber("+3333333333")
                .password("tempPass123")
                .role(FarmRole.WORKER)
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedTempPass");

        User savedUser = User.builder()
                .id(40L)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .password("encodedTempPass")
                .isActive(true)
                .build();

        FarmMember savedMember = FarmMember.builder()
                .id(400L)
                .farm(farm)
                .user(savedUser)
                .role(FarmRole.WORKER)
                .status(MembershipStatus.APPROVED)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(farmMemberRepository.save(any(FarmMember.class))).thenReturn(savedMember);

        WorkerResponse response = workerService.createWorker(1L, request, ownerUser.getEmail());

        assertNotNull(response);
        assertEquals("New Worker", response.getFullName());
        assertEquals("new.worker@example.com", response.getEmail());
        assertEquals(FarmRole.WORKER, response.getRole());
        assertEquals(MembershipStatus.APPROVED, response.getStatus());
    }

    @Test
    void createWorker_DuplicateEmail_ThrowsException() {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("New Worker")
                .email("existing@example.com")
                .phoneNumber("+3333333333")
                .password("tempPass123")
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateRecordException.class, () ->
                workerService.createWorker(1L, request, ownerUser.getEmail())
        );
    }

    @Test
    void createWorker_DuplicatePhone_ThrowsException() {
        WorkerRequest request = WorkerRequest.builder()
                .fullName("New Worker")
                .email("unique@example.com")
                .phoneNumber("+1111111111")
                .password("tempPass123")
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        assertThrows(DuplicateRecordException.class, () ->
                workerService.createWorker(1L, request, ownerUser.getEmail())
        );
    }

    @Test
    void updateWorker_Success() {
        WorkerUpdateRequest updateRequest = WorkerUpdateRequest.builder()
                .fullName("Updated Alice")
                .phoneNumber("+9999999999")
                .role(FarmRole.MANAGER)
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));

        when(farmMemberRepository.findById(200L)).thenReturn(Optional.of(workerMember));
        when(userRepository.save(any(User.class))).thenReturn(workerUser);
        when(farmMemberRepository.save(any(FarmMember.class))).thenReturn(workerMember);

        WorkerResponse response = workerService.updateWorker(1L, 200L, updateRequest, ownerUser.getEmail());

        assertNotNull(response);
        verify(userRepository).save(workerUser);
        verify(farmMemberRepository).save(workerMember);
    }

    @Test
    void deleteWorker_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(farmMemberRepository.findById(200L)).thenReturn(Optional.of(workerMember));
        when(farmMemberRepository.findByUserId(workerUser.getId())).thenReturn(Collections.emptyList());

        workerService.deleteWorker(1L, 200L, ownerUser.getEmail());

        verify(farmMemberRepository).delete(workerMember);
        verify(userRepository).delete(workerUser);
    }

    @Test
    void deleteWorker_CannotDeleteOwner_ThrowsValidationException() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(farmMemberRepository.findById(100L)).thenReturn(Optional.of(ownerMember));

        assertThrows(ValidationException.class, () ->
                workerService.deleteWorker(1L, 100L, ownerUser.getEmail())
        );
    }

    @Test
    void deleteWorker_CannotDeleteSelf_ThrowsValidationException() {
        FarmMember coOwnerMember = FarmMember.builder()
                .id(101L)
                .farm(farm)
                .user(ownerUser)
                .role(FarmRole.CO_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1L, 10L)).thenReturn(Optional.of(ownerMember));
        when(farmMemberRepository.findById(101L)).thenReturn(Optional.of(coOwnerMember));

        assertThrows(ValidationException.class, () ->
                workerService.deleteWorker(1L, 101L, ownerUser.getEmail())
        );
    }
}
