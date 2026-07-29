package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.dto.ChangePasswordRequest;
import com.poultry.backend.dto.LoginRequest;
import com.poultry.backend.dto.RegisterRequest;
import com.poultry.backend.dto.UserStatusRequest;
import com.poultry.backend.dto.UserUpdateRequest;
import com.poultry.backend.entity.Role;
import com.poultry.backend.entity.User;
import com.poultry.backend.repository.UserRepository;
import com.poultry.backend.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john.doe@example.com",
                "+1234567890",
                "Secure123!",
                Role.USER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("User account successfully registered")))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+1234567890")))
                .andExpect(jsonPath("$.data.role", is("USER")))
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    void testRegisterUser_DefaultRoleForcedForNonAdmin() throws Exception {
        // Non-authenticated/non-admin context registration
        RegisterRequest request = new RegisterRequest(
                "Jane Manager",
                "jane.manager@example.com",
                "+1112223333",
                "Password123!",
                Role.USER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role", is("USER")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testRegisterUser_AdminCanAssignRole() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Alex AdminCustom",
                "custom.admin@example.com",
                "+1444555666",
                "AdminSecure123!",
                Role.USER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role", is("USER")));
    }

    @Test
    void testRegisterUser_DuplicateEmail() throws Exception {
        // Create user first
        User user = User.builder()
                .fullName("Existing User")
                .email("duplicate@example.com")
                .phoneNumber("+1234567899")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        RegisterRequest request = new RegisterRequest(
                "New User",
                "duplicate@example.com",
                "+1987654321",
                "Password123!",
                Role.USER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void testRegisterUser_DuplicatePhone() throws Exception {
        // Create user first
        User user = User.builder()
                .fullName("Existing User")
                .email("one@example.com")
                .phoneNumber("+9999999999")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        RegisterRequest request = new RegisterRequest(
                "New User",
                "two@example.com",
                "+9999999999", // Duplicate phone number
                "Password123!",
                Role.USER
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Create user
        User user = User.builder()
                .fullName("Login User")
                .email("login@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("SecretPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("login@example.com", "SecretPassword123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.data.user.email", is("login@example.com")));

        // Verify last login timestamp was updated
        User updatedUser = userRepository.findByEmail("login@example.com").orElseThrow();
        assertNotNull(updatedUser.getLastLogin());
    }

    @Test
    void testLogin_Failure_BadCredentials() throws Exception {
        // Create user
        User user = User.builder()
                .fullName("Login User")
                .email("login@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("SecretPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("login@example.com", "wrong_password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        // Create user
        User user = User.builder()
                .fullName("Password User")
                .email("pass.change@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("OldPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        // Generate valid token for user
        String token = jwtUtils.generateToken(new com.poultry.backend.security.CustomUserDetails(user));

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewStrongPassword123!");

        mockMvc.perform(put("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // Verify password updated in repository
        User updatedUser = userRepository.findByEmail("pass.change@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NewStrongPassword123!", updatedUser.getPassword()));
    }

    @Test
    void testChangePassword_BadCurrentPassword() throws Exception {
        User user = User.builder()
                .fullName("Password User")
                .email("pass.change2@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("OldPassword123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        String token = jwtUtils.generateToken(new com.poultry.backend.security.CustomUserDetails(user));

        ChangePasswordRequest request = new ChangePasswordRequest("WrongOldPassword!", "NewStrongPassword123!");

        mockMvc.perform(put("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid current password")));
    }

    @Test
    void testGetMe_UnauthorizedAccess() throws Exception {
        // Request me without token
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMe_Success() throws Exception {
        User user = User.builder()
                .fullName("Profile User")
                .email("profile@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        String token = jwtUtils.generateToken(new com.poultry.backend.security.CustomUserDetails(user));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Profile User")))
                .andExpect(jsonPath("$.data.email", is("profile@example.com")));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testGetUsers_ForbiddenAccessForWorker() throws Exception {
        // WORKER attempts to access admin endpoints
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUsers_SuccessForAdmin() throws Exception {
        // Create user
        User user = User.builder()
                .fullName("Random Worker")
                .email("worker@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fullName", is("Random Worker")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminUserLifecycle_Success() throws Exception {
        // Create user
        User user = User.builder()
                .fullName("Manage Target")
                .email("target@example.com")
                .phoneNumber("+1234567890")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        // 1. Fetch by ID
        mockMvc.perform(get("/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName", is("Manage Target")));

        // 2. Put Update Details
        UserUpdateRequest update = new UserUpdateRequest("Updated Target", "target.new@example.com", "+1234567891", Role.USER);
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName", is("Updated Target")))
                .andExpect(jsonPath("$.data.email", is("target.new@example.com")))
                .andExpect(jsonPath("$.data.role", is("USER")));

        // 3. Patch Deactivate
        UserStatusRequest deactivate = new UserStatusRequest(false);
        mockMvc.perform(patch("/users/" + user.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)))
                .andExpect(jsonPath("$.message", containsString("deactivated")));

        // Verify state indeed changed in db
        User finalUser = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(finalUser.isActive());
    }
}
