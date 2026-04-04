package com.renoted.util;

import com.renoted.entity.User;
import com.renoted.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SECURITY UTILITIES - Helper Methods for Authentication
 *
 * PURPOSE:
 * Provides convenient methods to access current user information
 * from Spring Security's SecurityContext.
 *
 * WHAT IS SECURITYCONTEXT?
 * SecurityContext is thread-local storage that holds authentication
 * information for the current request.
 *
 * How it works:
 * 1. Request arrives with JWT token
 * 2. JwtAuthenticationFilter validates token
 * 3. Creates Authentication object
 * 4. Stores in SecurityContext
 * 5. Available throughout request lifecycle
 * 6. Cleared after request completes
 *
 * Thread-Local Behavior:
 * - Each thread (request) has its own SecurityContext
 * - Thread 1 → Alice's authentication
 * - Thread 2 → Bob's authentication
 * - No interference between requests
 * - Automatically managed by Spring
 *
 * @Component:
 * - Spring bean (singleton)
 * - Can be injected with @Autowired
 * - Available throughout application
 *
 * @RequiredArgsConstructor:
 * - Generates constructor for final fields
 * - Enables dependency injection
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    /**
     * USER REPOSITORY
     *
     * We need this to fetch full User entity from database.
     *
     * Why?
     * - SecurityContext only has username (from JWT)
     * - We need complete User entity (id, email, role, etc.)
     * - Query database to get full information
     */
    private final UserRepo userRepo;

    /**
     * GET CURRENT AUTHENTICATED USER
     *
     * Retrieves the User entity for currently logged-in user.
     *
     * WHEN TO USE:
     * - When creating notes (set owner)
     * - When querying notes (filter by owner)
     * - When updating/deleting (check ownership)
     * - Anywhere you need current user info
     *
     * HOW IT WORKS:
     *
     * 1. Get Authentication from SecurityContext
     *    - SecurityContextHolder.getContext() → Current thread's context
     *    - .getAuthentication() → Authentication object
     *    - Contains username from JWT token
     *
     * 2. Extract username
     *    - authentication.getName() → "alice"
     *    - This came from JWT token's "sub" claim
     *
     * 3. Load full User from database
     *    - userRepository.findByUsername("alice")
     *    - Returns complete User entity
     *    - Has id, email, password, role, etc.
     *
     * 4. Return User entity
     *    - Service layer can use it
     *    - Set as note owner
     *    - Check permissions
     *    - Filter queries
     *
     * FLOW EXAMPLE:
     *
     * Request: POST /api/notes (with JWT token)
     *    ↓
     * JwtAuthenticationFilter:
     *    - Validates token
     *    - Extracts username: "alice"
     *    - Sets authentication in context
     *    ↓
     * NoteService.createNote():
     *    - Calls SecurityUtils.getCurrentUser()
     *    - Gets User{id=1, username="alice", ...}
     *    - Sets note.setUser(alice)
     *    - Saves note with user_id=1
     *
     * THREAD SAFETY:
     *
     * SecurityContextHolder is thread-local:
     *
     * Request 1 (Alice):
     * Thread 1 → SecurityContext → Authentication(alice)
     * SecurityUtils.getCurrentUser() → User(alice)
     *
     * Request 2 (Bob) - at same time:
     * Thread 2 → SecurityContext → Authentication(bob)
     * SecurityUtils.getCurrentUser() → User(bob)
     *
     * No interference! Each request isolated!
     *
     * ERROR HANDLING:
     *
     * What if not authenticated?
     * - SecurityContext.getAuthentication() returns null
     * - This method throws NullPointerException
     * - Should only call on authenticated endpoints
     *
     * What if user deleted after token issued?
     * - Token still valid (not expired)
     * - But user no longer in database
     * - userRepository.findByUsername() returns empty
     * - .orElseThrow() throws RuntimeException
     * - Request fails (as it should!)
     *
     * USAGE EXAMPLE:
     *
     * In NoteService:
     *
     * public NoteDTO createNote(NoteDTO noteDTO) {
     *     // Get current user
     *     User currentUser = securityUtils.getCurrentUser();
     *
     *     // Create note
     *     Note note = new Note();
     *     note.setTitle(noteDTO.getTitle());
     *     note.setUser(currentUser); // ← Set owner!
     *
     *     Note saved = noteRepository.save(note);
     *     return convertToDTO(saved);
     * }
     *
     * In NoteService (filtering):
     *
     * public List<NoteDTO> getAllNotes() {
     *     // Get current user
     *     User currentUser = securityUtils.getCurrentUser();
     *
     *     // Get only their notes
     *     List<Note> notes = noteRepository.findByUser(currentUser);
     *
     *     return notes.stream()
     *         .map(this::convertToDTO)
     *         .collect(Collectors.toList());
     * }
     *
     * In NoteService (authorization):
     *
     * public void deleteNote(Long id) {
     *     // Get current user
     *     User currentUser = securityUtils.getCurrentUser();
     *
     *     // Get note only if belongs to user
     *     Note note = noteRepository.findByIdAndUser(id, currentUser)
     *         .orElseThrow(() -> new RuntimeException("Note not found or access denied"));
     *
     *     // Delete (we know user owns it)
     *     noteRepository.delete(note);
     * }
     *
     * @return User - Currently authenticated user entity
     * @throws RuntimeException if user not found in database
     * @throws NullPointerException if not authenticated
     */
    public User getCurrentUser() {
        /*
         * STEP 1: GET AUTHENTICATION FROM SECURITY CONTEXT
         *
         * SecurityContextHolder:
         * - Static class with thread-local storage
         * - Each thread has its own SecurityContext
         *
         * .getContext():
         * - Returns SecurityContext for current thread
         * - Never null (creates empty context if needed)
         *
         * .getAuthentication():
         * - Returns Authentication object
         * - null if not authenticated
         * - Contains user information if authenticated
         *
         * Authentication object contains:
         * - principal: UserDetails (user info)
         * - credentials: null (password not stored after auth)
         * - authorities: Collection<GrantedAuthority> (roles)
         * - authenticated: true/false
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /*
         * STEP 2: EXTRACT USERNAME FROM AUTHENTICATION
         *
         * authentication.getName():
         * - Returns principal name (username)
         * - Shortcut for: getPrincipal().getUsername()
         *
         * Where did this username come from?
         * 1. JWT token contains: { "sub": "alice" }
         * 2. JwtUtil.extractUsername() extracts: "alice"
         * 3. JwtAuthenticationFilter creates authentication
         * 4. Sets in SecurityContext
         * 5. Now we extract it back
         *
         * Why not just read from JWT every time?
         * - JWT already validated by filter
         * - Username stored in SecurityContext
         * - More efficient (no re-parsing)
         * - Consistent with Spring Security patterns
         */
        String username = authentication.getName();

        /*
         * STEP 3: FETCH FULL USER FROM DATABASE
         *
         * Why fetch from database?
         * - JWT only contains username (lightweight)
         * - We need complete User entity
         * - Need id, email, role, enabled status
         * - Need User object to set relationships
         *
         * userRepository.findByUsername(username):
         * - Queries database
         * - Returns Optional<User>
         * - Empty if user not found
         *
         * .orElseThrow():
         * - If user exists → Return User
         * - If user doesn't exist → Throw exception
         *
         * When would user not exist?
         * - User deleted after token was issued
         * - Token still valid (not expired)
         * - But user no longer in system
         * - Request should fail (user gone!)
         *
         * This is a feature:
         * - Deleted users can't access system
         * - Even with valid token
         * - Database is source of truth
         * - Security by design
         *
         * Exception message:
         * "User not found: alice"
         * - Indicates username from token
         * - Helps debugging
         * - Spring catches and returns 500 (or custom handler)
         */
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        /*
         * ALTERNATIVE IMPLEMENTATIONS:
         *
         * 1. Return Optional<User> (caller handles):
         * public Optional<User> getCurrentUser() {
         *     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         *     if (auth == null || auth.getName() == null) {
         *         return Optional.empty();
         *     }
         *     return userRepository.findByUsername(auth.getName());
         * }
         *
         * 2. Cache user in request scope (performance):
         * - Store in request attribute after first fetch
         * - Return cached user on subsequent calls
         * - Avoid multiple DB queries per request
         *
         * 3. Get User from Authentication principal:
         * UserDetails userDetails = (UserDetails) auth.getPrincipal();
         * - But this is Spring Security's UserDetails
         * - Not our User entity
         * - Would need to query database anyway
         *
         * Our approach is simple and clear:
         * - Straightforward
         * - Easy to understand
         * - Works well for most cases
         */
    }

    /**
     * GET CURRENT USERNAME
     *
     * Returns just the username without database query.
     *
     * Use this when you only need username, not full User entity.
     * More efficient (no database call).
     *
     * Usage:
     * String username = securityUtils.getCurrentUsername();
     * // "alice"
     *
     * When to use:
     * - Logging
     * - Audit trails
     * - Display in UI
     * - When full User entity not needed
     *
     * When NOT to use:
     * - Setting note owner (need User entity)
     * - Checking user ID (need User entity)
     * - Filtering by user (need User entity)
     *
     * @return String - Current user's username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * CHECK IF CURRENT USER IS AUTHENTICATED
     *
     * Returns true if user is authenticated, false otherwise.
     *
     * Usage:
     * if (securityUtils.isAuthenticated()) {
     *     // User is logged in
     * } else {
     *     // User is anonymous
     * }
     *
     * When to use:
     * - Conditional logic based on auth status
     * - Optional authentication endpoints
     * - Public endpoints that behave differently if authenticated
     *
     * @return boolean - true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - SECURITY UTILS
 * ═══════════════════════════════════════════════════════════
 *
 * 1. PURPOSE
 *    - Get current authenticated user
 *    - Bridge between Spring Security and our entities
 *    - Convenient utility for services
 *
 * 2. SECURITY CONTEXT
 *    - Thread-local storage
 *    - Each request has its own
 *    - Set by JwtAuthenticationFilter
 *    - Available throughout request
 *
 * 3. CURRENT USER
 *    - Extracted from SecurityContext
 *    - Fetched from database
 *    - Complete User entity returned
 *    - Used for ownership and authorization
 *
 * 4. THREAD SAFETY
 *    - Thread-local = isolated per request
 *    - No interference between requests
 *    - Safe for concurrent use
 *
 * 5. USAGE
 *    - Inject with @Autowired
 *    - Call getCurrentUser()
 *    - Use for setting owners
 *    - Use for filtering data
 *    - Use for authorization checks
 */