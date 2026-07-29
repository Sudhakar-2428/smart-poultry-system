package com.poultry.backend;

import com.poultry.backend.entity.Role;
import com.poultry.backend.entity.User;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.service.DatabaseCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseCleanupTest {

    @Autowired
    private DatabaseCleanupService databaseCleanupService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDatabaseCleanup_RemovesTargetUsers() {
        // Pre-populate target users in test database
        if (!userRepository.existsByEmail("sudhakarff96@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("Sudhakar Test 1")
                    .email("sudhakarff96@gmail.com")
                    .phoneNumber("+12345678901")
                    .password("encoded_pass_1")
                    .role(Role.USER)
                    .isActive(true)
                    .emailVerified(true)
                    .build());
        }

        if (!userRepository.existsByEmail("sudhakarshanmugasundar@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("Sudhakar Test 2")
                    .email("sudhakarshanmugasundar@gmail.com")
                    .phoneNumber("+12345678902")
                    .password("encoded_pass_2")
                    .role(Role.USER)
                    .isActive(true)
                    .emailVerified(true)
                    .build());
        }

        // Run cleanup
        databaseCleanupService.cleanTargetUsers();

        // Verify users no longer exist
        assertTrue(userRepository.findByEmail("sudhakarff96@gmail.com").isEmpty(),
                "userRepository.findByEmail('sudhakarff96@gmail.com') should return empty");

        assertTrue(userRepository.findByEmail("sudhakarshanmugasundar@gmail.com").isEmpty(),
                "userRepository.findByEmail('sudhakarshanmugasundar@gmail.com') should return empty");
    }
}
