package com.renoted.service;

import com.renoted.dto.NoteDTO;
import com.renoted.entity.Note;
import com.renoted.entity.Tag;
import com.renoted.entity.User;
import com.renoted.repo.NoteRepo;
import com.renoted.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.renoted.util.HtmlSanitizer;

/**
 * NoteService - Business Logic Layer
 *
 * Purpose: Contains all business logic for Note operations
 * Acts as intermediary between Controller and Repository
 *
 * What is a Service?
 * - Contains business logic (application-specific rules)
 * - Manages transactions (database operations that must succeed/fail together)
 * - Converts between Entity and DTO
 * - Coordinates multiple repository calls
 * - Handles exceptions and error cases
 *
 * Why do we need a Service layer?
 *
 * 1. SEPARATION OF CONCERNS
 *    - Controller: Handles HTTP (requests/responses)
 *    - Service: Business logic (what to do)
 *    - Repository: Database (how to persist)
 *
 * 2. REUSABILITY
 *    - Business logic in one place
 *    - Can be called from multiple controllers
 *    - Can be called from scheduled jobs, batch processes, etc.
 *
 * 3. TRANSACTION MANAGEMENT
 *    - Service methods are transactional
 *    - All database operations succeed or all fail
 *    - Data consistency guaranteed
 *
 * 4. TESTABILITY
 *    - Easy to unit test business logic
 *    - Mock repository in tests
 *    - No HTTP dependencies
 *
 * 5. SECURITY
 *    - Check permissions here
 *    - Validate business rules
 *    - Prevent unauthorized operations
 *
 * Example flow:
 * Frontend → Controller → Service → Repository → Database
 *                            ↑
 *                    Business logic here!
 */
@Service  // <-- Marks this as a Spring service component
//     Spring will:
//     1. Create instance of this class (bean)
//     2. Manage its lifecycle
//     3. Make it available for dependency injection
//     4. Enable transaction management

@Transactional  // <-- All methods in this class are transactional
//     What is a transaction?
//     - Group of database operations that must all succeed or all fail
//     - If ANY operation fails, ALL are rolled back
//     Example:
//       1. Save note
//       2. Update user stats
//       3. Create notification
//     If step 3 fails, steps 1 and 2 are undone (rolled back)
//     Database stays consistent!

public class NoteService {

    /**
     * Dependency Injection
     *
     * We need NoteRepo to access database
     * Spring will automatically inject (provide) it
     */
    private final NoteRepo noteRepo;
    // Why 'final'?
    // - Once set (by constructor), cannot change
    // - Thread-safe
    // - Makes this a required dependency
    // - Good practice!

    private final TagService tagService;

    private final SecurityUtils securityUtils;  // ← ADD THIS

    @Autowired  // <-- Tells Spring to inject NoteRepo
    //     Spring looks for a bean of type NoteRepo
    //     Finds our interface (Spring created implementation)
    //     Injects it into this constructor
    //     This is CONSTRUCTOR INJECTION (best practice)

    public NoteService(NoteRepo noteRepo, TagService tagService, SecurityUtils securityUtils) {
        this.noteRepo = noteRepo;
        this.tagService = tagService;
        this.securityUtils = securityUtils;  // ← ADD THIS
    }
    // Alternative: Field injection (less preferred)
    // @Autowired
    // private NoteRepo noteRepo;
    //
    // Why constructor injection is better?
    // - Makes dependencies explicit
    // - Easier to test (can pass mock in tests)
    // - Immutable (final field)
    // - Prevents null pointer exceptions

    /**
     * CREATE - Create a new note (with tags) - WITH USER OWNERSHIP
     *
     * Flow:
     * 1. Get current authenticated user
     * 2. Convert DTO to Entity
     * 3. Set user as owner
     * 4. Process tags
     * 5. Save note
     * 6. Convert back to DTO and return
     *
     * SECURITY:
     * - User is automatically set as owner
     * - Cannot create notes for other users
     * - Current user from JWT token
     *
     * @param noteDTO - Note data with tag IDs
     * @return Created note with ID, timestamps, and tag IDs
     */
    public NoteDTO createNote(NoteDTO noteDTO) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: GET CURRENT AUTHENTICATED USER
         * ═══════════════════════════════════════════════════════════
         *
         * Get the user who is making this request.
         *
         * How this works:
         * 1. User sends request with JWT token
         * 2. JwtAuthenticationFilter validated token
         * 3. Extracted username and set in SecurityContext
         * 4. SecurityUtils fetches User from database
         * 5. We get complete User entity
         *
         * This user will be the owner of the note!
         *
         * Example:
         * Alice sends: POST /api/notes { title: "My Note" }
         * Token contains: { sub: "alice" }
         * getCurrentUser() returns: User{id=1, username="alice", ...}
         *
         * Security implication:
         * - User cannot create notes for other users
         * - Owner is always current user
         * - Derived from authenticated token
         * - Cannot be spoofed or manipulated
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: CONVERT DTO TO ENTITY
         * ═══════════════════════════════════════════════════════════
         *
         * Create Note entity from DTO data.
         *
         * convertToEntity():
         * - Creates new Note object
         * - Sets title and content
         * - Sanitizes HTML (XSS protection)
         * - Does NOT set user yet
         */
        Note note = convertToEntity(noteDTO);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: SET USER AS OWNER - CRITICAL STEP!
         * ═══════════════════════════════════════════════════════════
         *
         * This is THE KEY LINE for ownership!
         *
         * note.setUser(currentUser):
         * - Links note to user
         * - Sets user_id foreign key in database
         * - Establishes ownership
         *
         * What happens in database:
         * INSERT INTO notes (title, content, user_id, ...)
         * VALUES ('My Note', 'Content', 1, ...)
         *                                  ↑
         *                          currentUser.getId()
         *
         * Before this line:
         * Note { title: "My Note", content: "...", user: null }
         *
         * After this line:
         * Note { title: "My Note", content: "...", user: User{id=1} }
         *
         * Why this is secure:
         * - User comes from SecurityContext (authenticated)
         * - Cannot be manipulated by client
         * - Always current logged-in user
         * - Database enforces NOT NULL constraint
         *
         * Without this line:
         * - Database would reject (user_id NOT NULL)
         * - ERROR: null value in column "user_id"
         */
        note.setUser(currentUser);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: HANDLE TAGS (if provided)
         * ═══════════════════════════════════════════════════════════
         *
         * Process tags exactly as before.
         * Tags are separate from ownership.
         *
         * Note: Tags don't have user ownership (shared across users)
         * Any user can use any tag
         * But notes belong to specific users
         */
        if (noteDTO.getTagIds() != null && !noteDTO.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : noteDTO.getTagIds()) {
                Tag tag = tagService.getTagEntityById(tagId);
                tags.add(tag);
            }
            note.setTags(tags);
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: SAVE NOTE TO DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * Persist note with user ownership.
         *
         * SQL executed:
         * INSERT INTO notes (title, content, user_id, created_at, updated_at)
         * VALUES ('My Note', 'Content...', 1, NOW(), NOW())
         * RETURNING id;
         *
         * Database state after save:
         * notes table:
         * ┌────┬──────────┬─────────┬─────────┬────────────┐
         * │ id │ title    │ content │ user_id │ created_at │
         * ├────┼──────────┼─────────┼─────────┼────────────┤
         * │ 1  │ My Note  │ ...     │ 1       │ 2026-03... │
         * └────┴──────────┴─────────┴─────────┴────────────┘
         *                             ↑
         *                      Ownership established!
         */
        Note savedNote = noteRepo.save(note);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 6: CONVERT TO DTO AND RETURN
         * ═══════════════════════════════════════════════════════════
         *
         * Convert saved note to DTO for API response.
         *
         * DTO will NOT include User object
         * (Frontend doesn't need it, they know it's theirs!)
         */
        return convertToDTO(savedNote);

        /*
         * ✅ NOTE CREATED WITH OWNERSHIP!
         *
         * Summary:
         * 1. ✅ Got current user from security context
         * 2. ✅ Created note entity
         * 3. ✅ Set user as owner
         * 4. ✅ Saved with user_id foreign key
         * 5. ✅ Returned DTO to frontend
         *
         * Security achieved:
         * - Note belongs to authenticated user
         * - User cannot create notes for others
         * - Ownership enforced at creation
         */
    }

    /**
     * READ - Get all notes (FILTERED BY CURRENT USER)
     *
     * Returns only notes belonging to the authenticated user.
     *
     * BEFORE (insecure):
     * - Returned ALL notes from ALL users
     * - Alice could see Bob's notes
     * - Data leakage!
     *
     * AFTER (secure):
     * - Returns only current user's notes
     * - Alice sees only Alice's notes
     * - Data isolation!
     *
     * @return List of notes belonging to current user
     */
    public List<NoteDTO> getAllNotes() {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: GET CURRENT USER
         * ═══════════════════════════════════════════════════════════
         *
         * Get authenticated user from security context.
         *
         * Example:
         * Alice requests: GET /api/notes
         * Token contains: { sub: "alice" }
         * getCurrentUser() returns: User{id=1, username="alice"}
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: QUERY ONLY USER'S NOTES
         * ═══════════════════════════════════════════════════════════
         *
         * Use repository method that filters by user.
         *
         * OLD (INSECURE):
         * List<Note> notes = noteRepo.findAll();
         * SQL: SELECT * FROM notes;
         * Result: ALL notes (Alice's + Bob's + Charlie's)
         *
         * NEW (SECURE):
         * List<Note> notes = noteRepo.findByUser(currentUser);
         * SQL: SELECT * FROM notes WHERE user_id = 1;
         * Result: ONLY Alice's notes
         *
         * Database result:
         * ┌────┬──────────┬─────────┐
         * │ id │ title    │ user_id │
         * ├────┼──────────┼─────────┤
         * │ 1  │ Alice#1  │ 1       │ ← Returned
         * │ 2  │ Alice#2  │ 1       │ ← Returned
         * │ 3  │ Bob#1    │ 2       │ ← NOT returned
         * │ 4  │ Bob#2    │ 2       │ ← NOT returned
         * └────┴──────────┴─────────┘
         *
         * Security achieved:
         * - User only sees their own data
         * - Cannot access other users' notes
         * - Database-level filtering
         * - Impossible to bypass
         */
        List<Note> notes = noteRepo.findByUser(currentUser);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: CONVERT TO DTOS
         * ═══════════════════════════════════════════════════════════
         *
         * Same conversion as before.
         * Map each Note entity to NoteDTO.
         */
        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        /*
         * ✅ USER-SPECIFIC DATA RETURNED!
         *
         * What changed:
         * - Before: findAll() → All notes
         * - After: findByUser(currentUser) → Only user's notes
         *
         * Security impact:
         * - Data isolation achieved
         * - Multi-tenant pattern
         * - Each user sees only their data
         */
    }

    /**
     * READ - Get single note by ID (WITH AUTHORIZATION)
     *
     * Returns note only if it belongs to current user.
     *
     * AUTHORIZATION:
     * - Check ownership before returning
     * - User can only access their own notes
     * - Returns 404 if doesn't exist OR doesn't belong to user
     *
     * @param id - Note ID
     * @return Note if found and belongs to user
     * @throws RuntimeException if not found or access denied
     */
    public NoteDTO getNoteById(Long id) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: GET CURRENT USER
         * ═══════════════════════════════════════════════════════════
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: FIND NOTE WITH AUTHORIZATION CHECK
         * ═══════════════════════════════════════════════════════════
         *
         * Use findByIdAndUser instead of findById!
         *
         * OLD (INSECURE):
         * Note note = noteRepo.findById(id).orElseThrow(...);
         * SQL: SELECT * FROM notes WHERE id = ?
         * Problem: Returns ANY note, even if belongs to another user!
         *
         * Then we'd check:
         * if (!note.getUser().getId().equals(currentUser.getId())) {
         *     throw new ForbiddenException();
         * }
         * Problem: We already fetched the data!
         *
         * NEW (SECURE):
         * Note note = noteRepo.findByIdAndUser(id, currentUser).orElseThrow(...);
         * SQL: SELECT * FROM notes WHERE id = ? AND user_id = ?
         * Result: Returns note ONLY if belongs to user
         *
         * Authorization Examples:
         *
         * Example 1 (Authorized):
         * Alice requests note 1
         * Note 1 belongs to Alice (user_id=1)
         * findByIdAndUser(1, alice) → Returns note ✅
         *
         * Example 2 (Unauthorized):
         * Alice requests note 3
         * Note 3 belongs to Bob (user_id=2)
         * findByIdAndUser(3, alice) → Returns empty ❌
         * orElseThrow() → Throws exception
         * User sees: "Note not found" (generic message for security)
         *
         * Why generic message?
         * - Don't reveal whether note exists
         * - Prevents information leakage
         * - "Note not found" could mean:
         *   a) Note doesn't exist at all
         *   b) Note exists but belongs to someone else
         * - Attacker can't tell which!
         *
         * Security benefit:
         * - Database does authorization
         * - Cannot fetch unauthorized data
         * - Fail-safe by design
         */
        Note note = noteRepo.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: RETURN DTO
         * ═══════════════════════════════════════════════════════════
         *
         * If we reach here:
         * - Note exists ✅
         * - Note belongs to current user ✅
         * - Safe to return
         */
        return convertToDTO(note);

        /*
         * ✅ AUTHORIZATION ENFORCED!
         *
         * What changed:
         * - Before: findById(id) → Any note
         * - After: findByIdAndUser(id, user) → Only user's note
         *
         * Security achieved:
         * - User cannot access others' notes
         * - Database-level authorization
         * - No information leakage
         */
    }

    /**
     * UPDATE - Update existing note (WITH AUTHORIZATION)
     *
     * Updates note only if it belongs to current user.
     *
     * AUTHORIZATION:
     * - Check ownership before updating
     * - User can only update their own notes
     * - Prevents unauthorized modifications
     *
     * @param id - Note ID
     * @param noteDTO - Updated note data
     * @return Updated note
     * @throws RuntimeException if not found or access denied
     */
    public NoteDTO updateNote(Long id, NoteDTO noteDTO) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: GET CURRENT USER
         * ═══════════════════════════════════════════════════════════
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: FIND NOTE WITH AUTHORIZATION
         * ═══════════════════════════════════════════════════════════
         *
         * Same authorization pattern as getNoteById.
         *
         * findByIdAndUser ensures:
         * - Note exists
         * - Note belongs to current user
         * - Both conditions must be true
         *
         * Authorization Example:
         * Bob tries to update Alice's note:
         * PUT /api/notes/1 (note 1 belongs to Alice)
         * findByIdAndUser(1, bob) → Empty
         * orElseThrow() → Exception
         * Result: Update denied ✅
         *
         * Alice updates her own note:
         * PUT /api/notes/1 (note 1 belongs to Alice)
         * findByIdAndUser(1, alice) → Returns note
         * Update proceeds ✅
         */
        Note existingNote = noteRepo.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: UPDATE NOTE FIELDS
         * ═══════════════════════════════════════════════════════════
         *
         * Update allowed fields.
         *
         * Note: We do NOT update user field!
         * - User is set at creation
         * - Cannot transfer notes between users
         * - Ownership is immutable
         *
         * Why?
         * - Prevents ownership manipulation
         * - Notes stay with original creator
         * - Clear audit trail
         *
         * If you wanted to allow transfer:
         * - Would need separate endpoint
         * - Extra authorization checks
         * - Audit logging
         * - Not implemented for security
         */
        existingNote.setTitle(noteDTO.getTitle());

        // Sanitize content for XSS protection
        String sanitizedContent = HtmlSanitizer.sanitize(noteDTO.getContent());
        existingNote.setContent(sanitizedContent);

        if (HtmlSanitizer.containsDangerousContent(noteDTO.getContent())) {
            System.out.println("🚨 XSS ATTACK BLOCKED on note update: " + noteDTO.getTitle());
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: UPDATE TAGS
         * ═══════════════════════════════════════════════════════════
         *
         * Tag update logic unchanged.
         */
        if (noteDTO.getTagIds() != null) {
            existingNote.getTags().clear();

            if (!noteDTO.getTagIds().isEmpty()) {
                Set<Tag> newTags = new HashSet<>();
                for (Long tagId : noteDTO.getTagIds()) {
                    Tag tag = tagService.getTagEntityById(tagId);
                    newTags.add(tag);
                }
                existingNote.setTags(newTags);
            }
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: SAVE AND RETURN
         * ═══════════════════════════════════════════════════════════
         *
         * Save updated note.
         *
         * Important: user_id remains unchanged!
         * - Not updated in UPDATE statement
         * - Ownership preserved
         */
        Note updatedNote = noteRepo.save(existingNote);
        return convertToDTO(updatedNote);

        /*
         * ✅ AUTHORIZED UPDATE COMPLETE!
         *
         * Security guarantees:
         * - Only owner can update
         * - Ownership cannot change
         * - Unauthorized updates blocked
         */
    }

    /**
     * DELETE - Delete note by ID (WITH AUTHORIZATION)
     *
     * Deletes note only if it belongs to current user.
     *
     * AUTHORIZATION:
     * - Check ownership before deleting
     * - User can only delete their own notes
     * - Prevents unauthorized deletions
     *
     * @param id - Note ID to delete
     * @throws RuntimeException if not found or access denied
     */
    public void deleteNote(Long id) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: GET CURRENT USER
         * ═══════════════════════════════════════════════════════════
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: FIND NOTE WITH AUTHORIZATION
         * ═══════════════════════════════════════════════════════════
         *
         * Verify ownership before deleting.
         *
         * OLD (INSECURE):
         * noteRepo.deleteById(id);
         * Problem: Deletes ANY note, regardless of owner!
         *
         * NEW (SECURE):
         * First check ownership, then delete.
         *
         * Authorization Example:
         * Charlie tries to delete Bob's note:
         * DELETE /api/notes/4 (note 4 belongs to Bob)
         * findByIdAndUser(4, charlie) → Empty
         * orElseThrow() → Exception
         * Result: Delete denied ✅
         *
         * Bob deletes his own note:
         * DELETE /api/notes/4
         * findByIdAndUser(4, bob) → Returns note
         * Delete proceeds ✅
         */
        Note note = noteRepo.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: DELETE NOTE
         * ═══════════════════════════════════════════════════════════
         *
         * If we reach here:
         * - Note exists ✅
         * - Note belongs to current user ✅
         * - Safe to delete
         *
         * Database action:
         * DELETE FROM notes WHERE id = ?
         *
         * Cascade behavior:
         * - Entries in note_tags table also deleted (join table)
         * - Tags themselves remain (other notes might use them)
         * - Only this note and its relationships deleted
         */
        noteRepo.delete(note);

        /*
         * ✅ AUTHORIZED DELETE COMPLETE!
         *
         * Security guarantees:
         * - Only owner can delete
         * - Cannot delete others' notes
         * - Database enforces ownership
         */
    }

    /**
     * SEARCH - Search notes by keyword (WITHIN USER'S NOTES ONLY)
     *
     * Searches only within current user's notes.
     *
     * BEFORE:
     * - Searched ALL notes (all users)
     * - Could see others' notes in search results
     *
     * AFTER:
     * - Searches only current user's notes
     * - Private search results
     *
     * @param query - Search keyword
     * @return List of matching notes belonging to user
     */
    public List<NoteDTO> searchNotes(String query) {
        /*
         * ═══════════════════════════════════════════════════════════
         * HANDLE EMPTY SEARCH
         * ═══════════════════════════════════════════════════════════
         *
         * If no query, return all user's notes.
         */
        if (query == null || query.isBlank()) {
            return getAllNotes(); // Already filtered by user!
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * SEARCH WITHIN USER'S NOTES
         * ═══════════════════════════════════════════════════════════
         */
        User currentUser = securityUtils.getCurrentUser();
        String trimmedQuery = query.trim();

        /*
         * OLD (INSECURE):
         * List<Note> notes = noteRepo
         *     .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
         *         trimmedQuery, trimmedQuery
         *     );
         * SQL: SELECT * FROM notes WHERE title ILIKE '%query%' OR content ILIKE '%query%'
         * Problem: Searches ALL users' notes!
         *
         * NEW (SECURE):
         * Search in title AND content, but only for current user's notes.
         *
         * We need to combine:
         * - Title search within user's notes
         * - Content search within user's notes
         *
         * Approach: Query both, combine results, remove duplicates
         */
        List<Note> titleMatches = noteRepo
                .findByTitleContainingIgnoreCaseAndUser(trimmedQuery, currentUser);

        List<Note> contentMatches = noteRepo
                .findByContentContainingIgnoreCaseAndUser(trimmedQuery, currentUser);

        /*
         * Combine results and remove duplicates
         *
         * If a note matches in both title AND content:
         * - It appears in both lists
         * - Use Set to eliminate duplicates
         * - Convert back to List
         */
        Set<Note> combinedResults = new HashSet<>();
        combinedResults.addAll(titleMatches);
        combinedResults.addAll(contentMatches);

        List<Note> notes = new ArrayList<>(combinedResults);

        /*
         * ═══════════════════════════════════════════════════════════
         * CONVERT TO DTOS
         * ═══════════════════════════════════════════════════════════
         */
        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        /*
         * ✅ USER-SCOPED SEARCH COMPLETE!
         *
         * Security achieved:
         * - Search only user's notes
         * - Cannot find others' notes
         * - Private search results
         *
         * Example:
         * Database has:
         * - Alice's note: "Java Tutorial"
         * - Bob's note: "Java Guide"
         *
         * Alice searches "Java":
         * - Returns: ["Java Tutorial"] ✅
         * - Does NOT return Bob's note ✅
         */
    }

    /**
     * FILTER - Get notes by tag (WITHIN USER'S NOTES ONLY)
     *
     * Returns user's notes that have the specified tag.
     *
     * BEFORE:
     * - Returned ALL notes with tag (all users)
     *
     * AFTER:
     * - Returns only current user's notes with tag
     *
     * @param tagId - Tag ID to filter by
     * @return List of user's notes with this tag
     */
    public List<NoteDTO> filterByTag(Long tagId) {
        /*
         * ═══════════════════════════════════════════════════════════
         * GET CURRENT USER
         * ═══════════════════════════════════════════════════════════
         */
        User currentUser = securityUtils.getCurrentUser();

        /*
         * ═══════════════════════════════════════════════════════════
         * FILTER BY TAG WITHIN USER'S NOTES
         * ═══════════════════════════════════════════════════════════
         *
         * We need notes that:
         * 1. Belong to current user (user_id = currentUser.id)
         * 2. Have the specified tag (in note_tags table)
         *
         * OLD (INSECURE):
         * List<Note> notes = noteRepo.findByTagId(tagId);
         * SQL: SELECT n.* FROM notes n
         *      JOIN note_tags nt ON n.id = nt.note_id
         *      WHERE nt.tag_id = ?
         * Problem: Returns ALL notes with tag (all users)!
         *
         * NEW (SECURE):
         * Get all user's notes, then filter by tag.
         *
         * This is a temporary solution.
         * Better: Create custom repository method
         * findByUserAndTagId(User user, Long tagId)
         *
         * For now, we'll:
         * 1. Get all user's notes
         * 2. Filter in Java for notes with this tag
         */
        List<Note> allUserNotes = noteRepo.findByUser(currentUser);

        /*
         * Filter notes that have the specified tag
         *
         * note.getTags() → Set<Tag>
         * tags.stream().anyMatch(...) → Check if any tag has this ID
         */
        List<Note> filteredNotes = allUserNotes.stream()
                .filter(note -> note.getTags().stream()
                        .anyMatch(tag -> tag.getId().equals(tagId)))
                .collect(Collectors.toList());

        /*
         * ═══════════════════════════════════════════════════════════
         * CONVERT TO DTOS
         * ═══════════════════════════════════════════════════════════
         */
        return filteredNotes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        /*
         * ✅ USER-SCOPED TAG FILTER COMPLETE!
         *
         * Security achieved:
         * - Only user's notes returned
         * - Tag filter within user's data
         * - Cannot see others' tagged notes
         *
         * Performance Note:
         * Current implementation:
         * - Fetches all user notes
         * - Filters in Java
         * - Works fine for small datasets
         *
         * Better (for large datasets):
         * Create repository method:
         * @Query("SELECT n FROM Note n JOIN n.tags t WHERE n.user = :user AND t.id = :tagId")
         * List<Note> findByUserAndTagId(@Param("user") User user, @Param("tagId") Long tagId);
         *
         * This pushes filtering to database (more efficient)
         */
    }

    /**
     * HELPER - Convert Entity to DTO (including tag IDs)
     */
    private NoteDTO convertToDTO(Note note) {
        // Convert tags to list of IDs
        List<Long> tagIds = note.getTags() != null ?
                note.getTags().stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList())
                : new ArrayList<>();

        // Use constructor WITH tagIds
        return new NoteDTO(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                tagIds  // ← Pass tag IDs here
        );
    }

    /**
     * HELPER METHOD: Convert DTO to Entity
     *
     * Used when creating or updating notes
     *
     * @param noteDTO - DTO from API request
     * @return Entity ready to save to database
     */
    private Note convertToEntity(NoteDTO noteDTO) {
        // Create new Note entity
        Note note = new Note();

        // Set fields from DTO
        // Note: We DON'T set ID for new notes (it's null, database generates it)
        // We also DON'T set timestamps (database handles them)
        if (noteDTO.getId() != null) {
            note.setId(noteDTO.getId());  // For updates
        }
        note.setTitle(noteDTO.getTitle());

        // ⚠️ SECURITY: Sanitize HTML to prevent XSS
        String sanitizedContent = HtmlSanitizer.sanitize(noteDTO.getContent());
        note.setContent(sanitizedContent);

        if (HtmlSanitizer.containsDangerousContent(noteDTO.getContent())) {
            System.out.println("🚨 XSS ATTACK BLOCKED on note: " + noteDTO.getTitle());
        }

        return note;
    }

    /**
     * WHY WE CONVERT BETWEEN DTO AND ENTITY:
     *
     * 1. LAYER SEPARATION
     *    - Controller layer: Works with DTOs (API contracts)
     *    - Service layer: Converts between DTO and Entity
     *    - Repository layer: Works with Entities (database)
     *
     * 2. SECURITY
     *    - DTOs control what data is exposed to API
     *    - Entities may have sensitive internal fields
     *    - Example: password, internalId, deletedAt
     *
     * 3. VALIDATION
     *    - DTOs have validation rules (@NotBlank, @Size)
     *    - Entities have database constraints (@Column)
     *    - Different concerns, different objects
     *
     * 4. FLEXIBILITY
     *    - Can change database structure without breaking API
     *    - Can change API without changing database
     *    - Decoupled evolution
     *
     * 5. API VERSIONING
     *    - Can have NoteDTO_v1, NoteDTO_v2
     *    - Same entity, different API representations
     *    - Backwards compatibility
     */
}