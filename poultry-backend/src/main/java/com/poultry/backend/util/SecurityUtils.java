package com.poultry.backend.util;

import com.poultry.backend.security.CustomUserDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@UtilityClass
public class SecurityUtils {

    /**
     * Retrieve the currently authenticated UserDetails principal.
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) principal);
        }
        return Optional.empty();
    }

    /**
     * Retrieve username of the currently logged-in user.
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUserDetails().map(CustomUserDetails::getUsername);
    }

    /**
     * Check if the current user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Check if the current logged-in user has a specific role.
     */
    public static boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        // Normalize role name (ensure starts with ROLE_)
        String targetRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(targetRole));
    }
}
