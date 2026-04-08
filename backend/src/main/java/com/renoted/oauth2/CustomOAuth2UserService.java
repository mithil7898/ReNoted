package com.renoted.oauth2;

import com.renoted.entity.User;
import com.renoted.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * CustomOAuth2UserService - OAuth2 Business Logic Layer
 *
 * Purpose: Handles user data received from OAuth2 providers (Google)
 * Acts as a bridge between Google authentication and our database
 *
 * What is OAuth2UserService?
 * - Interface provided by Spring Security
 * - Responsible for loading user details from OAuth2 provider
 * - Called AFTER user successfully logs in with Google
 *
 * Why do we need a CUSTOM implementation?
 *
 * 1. DEFAULT BEHAVIOR (Not enough)
 *    - Spring creates a temporary OAuth2User
 *    - Does NOT store user in our database
 *    - No link to our User entity ❌
 *
 * 2. OUR CUSTOM BEHAVIOR
 *    - Extract user details from Google response
 *    - Check if user exists in our database
 *    - If not → create new user
 *    - Return user to Spring Security
 *
 * 3. ACCOUNT LINKING
 *    - Same email = same user
 *    - Prevent duplicate accounts
 *    - Seamless login experience
 *
 * Example flow:
 * Frontend → Google Login → Google → Backend Callback
 *                                      ↓
 *                            CustomOAuth2UserService (THIS CLASS)
 *                                      ↓
 *                            Database (find/create user)
 *                                      ↓
 *                            Return OAuth2User → SuccessHandler
 */
@Service  // <-- Marks this as a Spring service component
// Spring will:
// 1. Create instance of this class
// 2. Manage lifecycle
// 3. Inject dependencies
// 4. Use it during OAuth2 login flow

public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    /**
     * Dependency Injection
     *
     * We need UserRepository to:
     * - Check if user exists
     * - Save new users
     */
    private final UserRepo userRepo;

    @Autowired
    public CustomOAuth2UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * CORE METHOD - loadUser()
     *
     * This method is called AUTOMATICALLY by Spring Security
     * after successful Google authentication.
     *
     * Flow:
     * 1. Google authenticates user
     * 2. Google sends authorization code to backend
     * 3. Spring exchanges code for access token
     * 4. Spring fetches user info from Google
     * 5. THIS METHOD IS CALLED with user info
     *
     * @param userRequest - contains access token & client info
     * @return OAuth2User - authenticated user
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: FETCH USER DETAILS FROM GOOGLE
         * ═══════════════════════════════════════════════════════════
         *
         * DefaultOAuth2UserService:
         * - Uses access token from userRequest
         * - Calls Google API: https://www.googleapis.com/oauth2/v3/userinfo
         * - Retrieves user profile data
         *
         * Example response from Google:
         * {
         *   "sub": "123456789",
         *   "email": "user@gmail.com",
         *   "name": "John Doe",
         *   "picture": "https://..."
         * }
         */
        OAuth2User oAuth2User = super.loadUser(userRequest);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: EXTRACT USER INFORMATION
         * ═══════════════════════════════════════════════════════════
         *
         * Extract required fields from Google response.
         *
         * getAttribute("email"):
         * - Returns user's email address
         * - This will be our UNIQUE identifier
         *
         * getAttribute("name"):
         * - User's full name from Google profile
         *
         * IMPORTANT:
         * Email is the KEY for account linking!
         */
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: CHECK IF USER EXISTS IN DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * We use email as unique identifier.
         *
         * Why email?
         * - Google guarantees verified email
         * - Unique per user
         * - Consistent across logins
         *
         * Example:
         * User logs in first time → create account
         * User logs in again → find existing account
         */
        Optional<User> userOptional = userRepo.findByEmail(email);

        User user;

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: HANDLE EXISTING VS NEW USER
         * ═══════════════════════════════════════════════════════════
         */

        if (userOptional.isPresent()) {

            /*
             * EXISTING USER FLOW
             *
             * - User already registered (via Google or normal signup)
             * - We simply log them in
             * - No need to create new account
             */
            user = userOptional.get();

        } else {

            /*
             * NEW USER FLOW
             *
             * - First time login via Google
             * - Create new user in database
             *
             * Important decisions:
             * - username = email (simple approach)
             * - password = "" (OAuth users don't need password)
             * - role = ROLE_USER
             */
            user = new User();

            user.setEmail(email);
            user.setUsername(email);
            user.setFullName(name);

            // No password for OAuth users
            user.setPassword("");

            // Default role
            user.setRole("ROLE_USER");

            /*
             * SAVE USER TO DATABASE
             *
             * SQL:
             * INSERT INTO users (email, username, full_name, role)
             * VALUES ('user@gmail.com', 'user@gmail.com', 'John Doe', 'ROLE_USER');
             */
            userRepo.save(user);
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: RETURN OAuth2User TO SPRING SECURITY
         * ═══════════════════════════════════════════════════════════
         *
         * This object will be:
         * - Stored in SecurityContext
         * - Passed to SuccessHandler
         *
         * IMPORTANT:
         * We are NOT returning our User entity here
         * We return OAuth2User (Spring Security standard)
         *
         * Later:
         * SuccessHandler → extract email → fetch User → generate JWT
         */
        return oAuth2User;

        /*
         * ✅ OAUTH2 USER PROCESSING COMPLETE!
         *
         * Summary:
         * 1. ✅ Fetched user info from Google
         * 2. ✅ Extracted email & name
         * 3. ✅ Checked database for existing user
         * 4. ✅ Created new user if needed
         * 5. ✅ Returned OAuth2User to Spring
         *
         * Result:
         * - User is now in our system
         * - Ready for JWT generation
         * - Seamless login experience
         */
    }
}