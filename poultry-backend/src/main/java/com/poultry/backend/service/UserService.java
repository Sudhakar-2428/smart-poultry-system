package com.poultry.backend.service;

import com.poultry.backend.dto.ChangePasswordRequest;
import com.poultry.backend.dto.UserDto;
import com.poultry.backend.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserDto getCurrentUser(String email);
    void changePassword(String email, ChangePasswordRequest request);
    List<UserDto> getAllUsers();
    UserDto getUserById(Long id);
    UserDto updateUser(Long id, UserUpdateRequest request);
    UserDto updateUserStatus(Long id, Boolean active);
}
