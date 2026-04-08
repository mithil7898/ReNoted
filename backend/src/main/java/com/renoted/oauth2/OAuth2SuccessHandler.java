package com.renoted.oauth2;

import com.renoted.entity.User;
import com.renoted.repo.UserRepo;
import com.renoted.util.JwtUtil;
import com.renoted.service.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * OAuth2SuccessHandler - Post Authentication Logic
 *
 * Purpose:
 * Handles what happens AFTER successful Google login.
 *
 * This is where we:
 * - Convert OAuth2 login → JWT-based system
 * - Generate access token (JWT)
 * - Generate refresh token (DB-backed)
 * - Send tokens to frontend
 *
 * Why do we need this?
 *
 * 1. OAuth2 ≠ Our Auth System
 *    - Google authenticates user
 *    - But our app uses JWT
 *    - We need to "bridge" the two systems
 *
 * 2. Token Generation Point
 *    - This is the FIRST place user is authenticated
 *    - Perfect place to generate tokens
 *
 * 3. Centralized Logic
 *    - All OAuth logins handled here
 *    - Clean separation from controllers/services
 *
 * Flow:
 * Google Login → CustomOAuth2UserService → THIS HANDLER
 *                                               ↓
 *                                   Generate JWT + Refresh Token
 *                                               ↓
 *                                   Redirect to frontend with tokens
 */
@Component  // <-- Register as Spring Bean
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * Dependencies
     *
     * We reuse existing Phase 1 components
     */

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepo userRepo;

    @Autowired
    public OAuth2SuccessHandler(JwtUtil jwtUtil,
                                RefreshTokenService refreshTokenService,
                                UserRepo userRepo) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepo = userRepo;
    }

    /**
     * CORE METHOD - onAuthenticationSuccess()
     *
     * Called AUTOMATICALLY after successful OAuth2 login.
     *
     * This is equivalent to:
     * - login() method in AuthController (for username/password)
     *
     * @param request        HTTP request
     * @param response       HTTP response
     * @param authentication contains authenticated user
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: EXTRACT OAUTH2 USER
         * ═══════════════════════════════════════════════════════════
         *
         * authentication.getPrincipal():
         * - Contains the authenticated user from Google
         * - Type: OAuth2User
         *
         * This object contains:
         * - email
         * - name
         * - profile picture
         */
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: EXTRACT USER EMAIL
         * ═══════════════════════════════════════════════════════════
         *
         * We use email as unique identifier.
         *
         * IMPORTANT:
         * This MUST match what we used in CustomOAuth2UserService
         */
        String email = oAuth2User.getAttribute("email");

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: FETCH USER FROM DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * Why fetch again?
         * - OAuth2User is NOT our entity
         * - We need our User entity for JWT generation
         */
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: CONVERT USER → USERDETAILS (ADAPTER STEP)
         * ═══════════════════════════════════════════════════════════
         *
         * Problem:
         * - JwtUtil expects UserDetails
         * - We have our User entity
         *
         * Solution:
         * - Convert User → UserDetails manually
         *
         * We use Spring Security's built-in User class:
         * org.springframework.security.core.userdetails.User
         *
         * This is a lightweight adapter.
         */

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),          // principal (username/email)
                user.getPassword() != null ? user.getPassword() : "",  // password (safe fallback)
                List.of(new SimpleGrantedAuthority(user.getRole()))   // roles/authorities
        );

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: GENERATE ACCESS TOKEN (JWT)
         * ═══════════════════════════════════════════════════════════
         *
         * Reusing Phase 1 logic.
         *
         * JWT contains:
         * - subject (username/email)
         * - expiration
         * - signature
         *
         * Stateless authentication!
         */
        String accessToken = jwtUtil.generateToken(userDetails);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 6: GENERATE REFRESH TOKEN (DATABASE)
         * ═══════════════════════════════════════════════════════════
         *
         * RefreshTokenService:
         * - Creates token
         * - Stores in DB
         * - Sets expiration
         */
        String refreshToken = refreshTokenService
                .createRefreshToken(user)
                .getToken();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 7: PREPARE REDIRECT URL
         * ═══════════════════════════════════════════════════════════
         *
         * We send tokens to frontend via query params.
         *
         * Example:
         * http://localhost:5173/oauth2/success?
         *   accessToken=abc123&
         *   refreshToken=xyz456
         *
         * Frontend will:
         * - Extract tokens
         * - Store them (localStorage or cookies)
         */
        String redirectUrl = "http://localhost:5173/oauth2/success"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken;

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 8: REDIRECT TO FRONTEND
         * ═══════════════════════════════════════════════════════════
         *
         * This sends the user back to frontend.
         *
         * Browser automatically navigates to:
         * /oauth2/success with tokens
         */
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        /*
         * ✅ OAUTH2 LOGIN COMPLETE!
         *
         * Summary:
         * 1. ✅ Extracted user from Google
         * 2. ✅ Found user in database
         * 3. ✅ Generated JWT (access token)
         * 4. ✅ Generated refresh token (DB)
         * 5. ✅ Redirected to frontend with tokens
         *
         * Final Result:
         * - User is authenticated
         * - Tokens are issued
         * - Frontend receives tokens
         * - System is now consistent with Phase 1
         */
    }
}