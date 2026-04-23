package com.renoted.controller;

import com.renoted.dto.*;
import com.renoted.entity.RefreshToken;
import com.renoted.entity.User;
import com.renoted.service.RefreshTokenService;
import com.renoted.service.UserService;
import com.renoted.util.JwtUtil;
import com.renoted.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AUTH CONTROLLER (UPDATED WITH ApiResponse)
 *
 * PURPOSE:
 * - Handles authentication endpoints
 * - Wraps all responses in ApiResponse (STANDARD FORMAT)
 *
 * IMPORTANT:
 * - Service layer remains unchanged
 * - Only Controller handles ApiResponse
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    /**
     * REGISTER USER
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        // Step 1: Register user
        UserDTO user = userService.registerUser(request);

        // Step 2: Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        // Step 3: Generate access token
        String accessToken = jwtUtil.generateToken(userDetails);

        // Step 4: Generate refresh token
        User userEntity = userService.getUserEntityByUsername(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userEntity);

        // Step 5: Build response DTO
        AuthResponse authResponse = new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user
        );

        // Step 6: Wrap in ApiResponse
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully", authResponse)
        );
    }

    /**
     * LOGIN USER
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        // Step 1: Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Step 2: Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Step 3: Generate access token
        String accessToken = jwtUtil.generateToken(userDetails);

        // Step 4: Generate refresh token
        User userEntity = userService.getUserEntityByUsername(request.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userEntity);

        // Step 5: Get user DTO
        UserDTO user = userService.findByUsername(request.getUsername());

        // Step 6: Build response DTO
        AuthResponse authResponse = new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user
        );

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authResponse)
        );
    }

    /**
     * REFRESH TOKEN
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {

        String requestRefreshToken = request.getRefreshToken();

        // Step 1: Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(requestRefreshToken);

        // Step 2: Get user
        User user = refreshToken.getUser();

        // Step 3: Generate new access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtUtil.generateToken(userDetails);

        TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken);

        return ResponseEntity.ok(
                ApiResponse.success("Access token refreshed successfully", response)
        );
    }

    /**
     * LOGOUT
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRefreshRequest request
    ) {

        // Step 1: Revoke refresh token
        refreshTokenService.revokeToken(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully", null)
        );
    }

    /**
     * GET CURRENT USER
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {

        // Step 1: Get authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Step 2: Extract username
        String username = authentication.getName();

        // Step 3: Fetch user
        UserDTO user = userService.findByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", user)
        );
    }
}