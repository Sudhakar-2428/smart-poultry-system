package com.poultry.backend.service;

import com.poultry.backend.dto.DeleteFarmRequest;
import com.poultry.backend.dto.DeleteFarmResponse;
import com.poultry.backend.dto.FarmDeleteCheckResponse;
import com.poultry.backend.entity.Farm;
import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.FarmRole;
import com.poultry.backend.entity.User;
import com.poultry.backend.exception.*;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.impl.DeleteFarmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteFarmServiceTest {

    @Mock private FarmRepository farmRepository;
    @Mock private FarmMemberRepository farmMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SalesOrderItemRepository salesOrderItemRepository;
    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private LedgerTransactionRepository ledgerTransactionRepository;
    @Mock private LedgerAccountRepository ledgerAccountRepository;
    @Mock private IncomeCategoryRepository incomeCategoryRepository;
    @Mock private ExpenseCategoryRepository expenseCategoryRepository;
    @Mock private FeedConsumptionRepository feedConsumptionRepository;
    @Mock private FeedPurchaseRepository feedPurchaseRepository;
    @Mock private FeedItemRepository feedItemRepository;
    @Mock private FeedSupplierRepository feedSupplierRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private EggRecordRepository eggRecordRepository;
    @Mock private EggBatchRepository eggBatchRepository;
    @Mock private HatchResultRepository hatchResultRepository;
    @Mock private ChickGrowthRecordRepository chickGrowthRecordRepository;
    @Mock private BrooderBatchRepository brooderBatchRepository;
    @Mock private IncubatorBatchRepository incubatorBatchRepository;
    @Mock private BreedingPairRepository breedingPairRepository;
    @Mock private ChickenRepository chickenRepository;
    @Mock private FarmSettingRepository farmSettingRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DeleteFarmServiceImpl deleteFarmService;

    private User ownerUser;
    private Farm farm;
    private FarmMember ownerMember;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(1L).email("owner@test.com").password("encodedSecret").build();
        farm = Farm.builder().id(10L).name("Test Farm").build();
        ownerMember = FarmMember.builder().id(100L).farm(farm).user(ownerUser).role(FarmRole.PRIMARY_OWNER).build();
    }

    @Test
    void testCheckDeleteEligibility_Success() {
        try (MockedStatic<com.poultry.backend.util.SecurityUtils> securityUtils = Mockito.mockStatic(com.poultry.backend.util.SecurityUtils.class)) {
            securityUtils.when(com.poultry.backend.util.SecurityUtils::getCurrentUsername).thenReturn(Optional.of("owner@test.com"));
            when(farmRepository.existsById(10L)).thenReturn(true);
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));
            when(farmMemberRepository.findByFarmIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMember));
            when(farmMemberRepository.findByFarmId(10L)).thenReturn(List.of(ownerMember));

            FarmDeleteCheckResponse response = deleteFarmService.checkDeleteEligibility(10L);

            assertTrue(response.isCanDelete());
            assertEquals(0, response.getWorkerCount());
        }
    }

    @Test
    void testDeleteFarm_Success() {
        try (MockedStatic<com.poultry.backend.util.SecurityUtils> securityUtils = Mockito.mockStatic(com.poultry.backend.util.SecurityUtils.class)) {
            securityUtils.when(com.poultry.backend.util.SecurityUtils::getCurrentUsername).thenReturn(Optional.of("owner@test.com"));
            when(farmRepository.existsById(10L)).thenReturn(true);
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));
            when(farmMemberRepository.findByFarmIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMember));
            when(farmMemberRepository.findByFarmId(10L)).thenReturn(List.of(ownerMember));
            when(passwordEncoder.matches("Secret123!", "encodedSecret")).thenReturn(true);

            DeleteFarmRequest request = DeleteFarmRequest.builder()
                    .confirmationText("DELETE")
                    .password("Secret123!")
                    .build();

            DeleteFarmResponse response = deleteFarmService.deleteFarm(10L, request);

            assertTrue(response.isSuccess());
            verify(farmRepository).deleteById(10L);
            verify(userRepository).deleteById(1L);
        }
    }

    @Test
    void testDeleteFarm_WorkerConnected_ThrowsConflictException() {
        try (MockedStatic<com.poultry.backend.util.SecurityUtils> securityUtils = Mockito.mockStatic(com.poultry.backend.util.SecurityUtils.class)) {
            securityUtils.when(com.poultry.backend.util.SecurityUtils::getCurrentUsername).thenReturn(Optional.of("owner@test.com"));
            when(farmRepository.existsById(10L)).thenReturn(true);
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));
            when(farmMemberRepository.findByFarmIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMember));

            User worker = User.builder().id(2L).email("worker@test.com").build();
            FarmMember workerMember = FarmMember.builder().id(101L).farm(farm).user(worker).role(FarmRole.WORKER).build();
            when(farmMemberRepository.findByFarmId(10L)).thenReturn(List.of(ownerMember, workerMember));

            DeleteFarmRequest request = DeleteFarmRequest.builder()
                    .confirmationText("DELETE")
                    .password("Secret123!")
                    .build();

            assertThrows(ConflictException.class, () -> deleteFarmService.deleteFarm(10L, request));
        }
    }
}
