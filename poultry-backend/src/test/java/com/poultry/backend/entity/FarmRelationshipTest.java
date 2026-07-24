package com.poultry.backend.entity;

import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.FarmMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FarmRelationshipTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMemberRepository farmMemberRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;


    @Test
    public void testFarmToUserRelationships() {
        // 1. Create and save a User
        User user = User.builder()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .password("securePassword")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        assertNotNull(user.getId());

        // 2. Create and save a Farm
        Farm farm = Farm.builder()
                .name("Sunny Valley Poultry")
                .build();
        farm = farmRepository.save(farm);
        assertNotNull(farm.getId());
        assertNotNull(farm.getFarmUniqueId());
        assertNotNull(farm.getJoinCode());
        assertEquals(8, farm.getJoinCode().length()); // verify random join code generation

        // 3. Create and save a FarmMember (Relationship mapping user and farm)
        FarmMember membership = FarmMember.builder()
                .farm(farm)
                .user(user)
                .role(FarmRole.PRIMARY_OWNER)
                .status(MembershipStatus.APPROVED)
                .build();
        membership = farmMemberRepository.save(membership);
        assertNotNull(membership.getId());

        // Refresh entities from persistence context
        farmRepository.flush();
        userRepository.flush();
        farmMemberRepository.flush();
        entityManager.clear();


        // 4. Retrieve and verify the relationship mapping: One Farm -> Many Farm Members
        Farm retrievedFarm = farmRepository.findById(farm.getId()).orElseThrow();
        assertEquals(1, retrievedFarm.getMembers().size());
        assertEquals(membership.getId(), retrievedFarm.getMembers().get(0).getId());

        // 5. Retrieve and verify the relationship mapping: One User -> Many Farm Members (memberships)
        User retrievedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1, retrievedUser.getMemberships().size());
        assertEquals(membership.getId(), retrievedUser.getMemberships().get(0).getId());

        // 6. Verify FarmMember links back to Farm and User
        FarmMember retrievedMembership = farmMemberRepository.findById(membership.getId()).orElseThrow();
        assertEquals(retrievedFarm.getId(), retrievedMembership.getFarm().getId());
        assertEquals(retrievedUser.getId(), retrievedMembership.getUser().getId());
        assertEquals(FarmRole.PRIMARY_OWNER, retrievedMembership.getRole());
        assertEquals(MembershipStatus.APPROVED, retrievedMembership.getStatus());
    }
}
