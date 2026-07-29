package com.poultry.backend.security;

import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.MembershipStatus;
import com.poultry.backend.entity.User;
import com.poultry.backend.repository.FarmMemberRepository;
import com.poultry.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final FarmMemberRepository farmMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());
        String currentFarmRole = memberships.stream()
                .filter(fm -> fm.getStatus() == MembershipStatus.APPROVED)
                .map(fm -> fm.getRole().name())
                .findFirst()
                .orElse(null);

        return new CustomUserDetails(user, currentFarmRole);
    }
}
