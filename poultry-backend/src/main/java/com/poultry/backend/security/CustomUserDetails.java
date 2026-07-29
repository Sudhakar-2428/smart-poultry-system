package com.poultry.backend.security;

import com.poultry.backend.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final String currentFarmRole;

    public CustomUserDetails(User user) {
        this.user = user;
        this.currentFarmRole = null;
    }

    public CustomUserDetails(User user, String currentFarmRole) {
        this.user = user;
        this.currentFarmRole = currentFarmRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user != null && user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        if (currentFarmRole != null && !currentFarmRole.trim().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + currentFarmRole));
        } else {
            // Default to PRIMARY_OWNER and MANAGER authorities for users without explicitly restricted farm roles
            authorities.add(new SimpleGrantedAuthority("ROLE_PRIMARY_OWNER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getEmail() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user == null || user.isActive();
    }
}
