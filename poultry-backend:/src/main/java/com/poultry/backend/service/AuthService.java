package com.poultry.backend.service;

import com.poultry.backend.dto.*;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    UserDto register(RegisterRequest registerRequest);

    UserDto registerOwner(OwnerRegisterRequest request);
    UserDto registerWorker(WorkerRegisterRequest request);
    void verifyEmail(EmailVerificationRequest request);
    void joinFarm(JoinFarmRequest request);
}

