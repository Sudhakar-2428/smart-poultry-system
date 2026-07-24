package com.poultry.backend.service;

import com.poultry.backend.entity.Farm;
import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.Notification;
import com.poultry.backend.entity.User;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.FarmRepository;
import com.poultry.backend.repository.NotificationRepository;
import com.poultry.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleanupService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmRepository farmRepository;
    private final NotificationRepository notificationRepository;

    private static final List<String> TARGET_EMAILS = Arrays.asList(
            "sudhakarff96@gmail.com",
            "sudhakarshanmugasundar@gmail.com"
    );

    @Override
    @Transactional
    public void run(String... args) {
        // One-time cleanup completed. Manual trigger available via cleanTargetUsers().
    }

    @Transactional
    public void cleanTargetUsers() {
        log.info("Starting authentication database cleanup for target test accounts...");

        for (String email : TARGET_EMAILS) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("Found user to clean up: {} (ID: {})", user.getEmail(), user.getId());

                // 1. Clean farm memberships and empty farms associated with user
                List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());
                for (FarmMember member : memberships) {
                    Farm farm = member.getFarm();
                    log.info("Deleting farm membership for user: {} in farm ID: {}", user.getEmail(), farm.getId());
                    farmMemberRepository.delete(member);

                    // Check if farm has remaining members
                    List<FarmMember> remainingMembers = farmMemberRepository.findByFarmId(farm.getId());
                    boolean onlyTargetUser = remainingMembers.isEmpty() || 
                            remainingMembers.stream().allMatch(m -> m.getUser().getId().equals(user.getId()));
                    if (onlyTargetUser) {
                        log.info("Deleting empty farm owned by target user: {}", farm.getName());
                        farmRepository.delete(farm);
                    }
                }

                // 2. Remove notification records targeting user ID
                List<Notification> notifications = notificationRepository.findByTargetId(user.getId());
                if (!notifications.isEmpty()) {
                    log.info("Deleting {} notification records for user: {}", notifications.size(), user.getEmail());
                    notificationRepository.deleteAll(notifications);
                }

                // 3. Delete user record
                userRepository.delete(user);
                userRepository.flush();
                log.info("Successfully deleted user record for email: {}", email);
            } else {
                log.info("Target user not found (already clean): {}", email);
            }

            // 4. Verify user no longer exists
            Optional<User> checkOpt = userRepository.findByEmail(email);
            if (checkOpt.isPresent()) {
                log.error("CRITICAL: Verification failed! User still exists in database: {}", email);
                throw new IllegalStateException("Database cleanup failed for email: " + email);
            }
        }

        System.out.println("Deleted:");
        for (String email : TARGET_EMAILS) {
            System.out.println("- " + email);
        }
        System.out.println("\nDatabase cleanup completed successfully.");
        log.info("Database cleanup completed successfully for target emails.");
    }
}
