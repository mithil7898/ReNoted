package com.renoted.repo;

import com.renoted.entity.Note;
import com.renoted.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NoteRepository - Database Access Layer
 *
 * Purpose: Provides database operations for Note entity
 * This is a Spring Data JPA repository interface
 *
 * What is a Repository?
 * - Interface that handles database operations (CRUD)
 * - We DON'T write implementation - Spring generates it automatically!
 * - No SQL needed - Spring Data JPA creates queries for us
 *
 * How does this work?
 * 1. We extend JpaRepository<Note, Long>
 *    - Note = Entity type (what we're storing)
 *    - Long = Primary key type (ID type)
 * 2. Spring sees this interface at startup
 * 3. Spring creates implementation class automatically
 * 4. We can use it immediately!
 *
 * What methods do we get for FREE?
 * - save(note) → INSERT or UPDATE
 * - findById(id) → SELECT by ID
 * - findAll() → SELECT all records
 * - deleteById(id) → DELETE by ID
 * - count() → COUNT records
 * - existsById(id) → Check if exists
 * - And many more!
 *
 * Example usage (in service):
 * Note note = new Note("My Title", "My Content");
 * noteRepository.save(note);  // Saves to database
 *
 * List<Note> notes = noteRepository.findAll();  // Gets all notes
 *
 * Optional<Note> note = noteRepository.findById(1L);  // Finds by ID
 *
 * noteRepository.deleteById(1L);  // Deletes note
 */
@Repository  // <-- Marks this as a Spring repository bean
//     Spring will manage this as a component
//     Enables exception translation (SQLException → DataAccessException)
//     Optional but good practice

public interface NoteRepo extends JpaRepository<Note, Long> {
    // JpaRepository<Note, Long> provides:
    // - Note = The entity we're working with
    // - Long = The type of the entity's ID field

    /**
     * That's it! No methods needed!
     *
     * JpaRepository already provides these methods:
     *
     * BASIC CRUD:
     * - save(Note note) → Save or update note
     * - findById(Long id) → Find note by ID
     * - findAll() → Get all notes
     * - deleteById(Long id) → Delete note by ID
     * - delete(Note note) → Delete specific note
     * - count() → Count total notes
     * - existsById(Long id) → Check if note exists
     *
     * ADVANCED:
     * - findAll(Sort sort) → Get all notes, sorted
     * - findAll(Pageable pageable) → Get paginated notes
     * - saveAll(List<Note> notes) → Save multiple notes
     * - deleteAll() → Delete all notes
     *
     * We can ADD custom query methods if needed.
     * Spring Data JPA generates queries from method names!
     */

    /**
     * Search notes by title or content (case-insensitive)
     *
     * Why we need this:
     * - Users want to find notes quickly
     * - Search in both title and content
     * - Case-insensitive for better UX
     *
     * Method naming:
     * - findBy = SELECT query
     * - TitleContaining = title LIKE %?%
     * - Or = OR condition
     * - ContentContaining = content LIKE %?%
     * - IgnoreCase = LOWER() comparison
     *
     * Spring Data JPA generates:
     * SELECT * FROM notes
     * WHERE LOWER(title) LIKE LOWER('%?%')
     *    OR LOWER(content) LIKE LOWER('%?%')
     *
     * Example:
     * User searches "spring"
     * Finds: "Spring Boot Tutorial", "Getting Started with spring"
     *
     * @param titleQuery - Search term for title
     * @param contentQuery - Search term for content (same as titleQuery)
     * @return List of notes matching search
     */
    List<Note> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String titleQuery,
            String contentQuery
    );

    /**
     * Find notes by tag ID
     *
     * Why we need this:
     * - Filter notes by specific tag
     * - Example: Show all "Java" notes
     *
     * JPQL Query:
     * - SELECT n FROM Note n: Get notes
     * - JOIN n.tags t: Join with tags (Many-to-Many)
     * - WHERE t.id = :tagId: Filter by tag ID
     *
     * Spring generates SQL:
     * SELECT n.* FROM notes n
     * INNER JOIN note_tags nt ON n.id = nt.note_id
     * WHERE nt.tag_id = ?
     *
     * Example:
     * tagId = 1 (Java)
     * Returns all notes tagged with "Java"
     *
     * @param tagId - Tag ID to filter by
     * @return List of notes with this tag
     */
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE t.id = :tagId")
    List<Note> findByTagId(@Param("tagId") Long tagId);

    // CUSTOM QUERY METHODS (Optional - examples for future use)
    // Uncomment these when you need them:

    // Find notes by title (case-insensitive)
    // Spring generates: SELECT * FROM notes WHERE LOWER(title) LIKE LOWER(?)
    // List<Note> findByTitleContainingIgnoreCase(String title);

    // Find notes by exact title
    // Spring generates: SELECT * FROM notes WHERE title = ?
    // List<Note> findByTitle(String title);

    // Find notes ordered by creation date (newest first)
    // Spring generates: SELECT * FROM notes ORDER BY created_at DESC
    // List<Note> findAllByOrderByCreatedAtDesc();

    // Find notes created after a certain date
    // Spring generates: SELECT * FROM notes WHERE created_at > ?
    // List<Note> findByCreatedAtAfter(LocalDateTime date);

    // Count notes with specific title
    // Spring generates: SELECT COUNT(*) FROM notes WHERE title = ?
    // long countByTitle(String title);

    /**
     * FIND NOTES BY USER
     *
     * Retrieves all notes belonging to a specific user.
     *
     * Method Name Convention:
     * findBy + User (field name in Note entity)
     *
     * Generated SQL:
     * SELECT * FROM notes WHERE user_id = ?
     *
     * Why this is important:
     * - Each user should only see their own notes
     * - Core concept of data isolation
     * - Multi-tenant application pattern
     *
     * Usage:
     * User currentUser = getCurrentUser();
     * List<Note> myNotes = noteRepository.findByUser(currentUser);
     * // Returns only notes where user_id = currentUser.id
     *
     * Example:
     * User alice (id=1)
     * findByUser(alice) → Returns notes with user_id=1
     *
     * Security implication:
     * - Users can only query their own data
     * - Cannot see other users' notes
     * - Database-level data filtering
     *
     * @param user - The user whose notes to retrieve
     * @return List<Note> - All notes belonging to this user
     */
    List<Note> findByUser(User user);

    /**
     * FIND NOTE BY ID AND USER
     *
     * Retrieves a specific note only if it belongs to the user.
     *
     * Method Name Convention:
     * findBy + Id + And + User
     *
     * Generated SQL:
     * SELECT * FROM notes WHERE id = ? AND user_id = ?
     *
     * Why this is critical for security:
     * - Prevents unauthorized access to notes
     * - User can only get their own notes
     * - Returns empty if note doesn't belong to user
     *
     * Usage (Authorization):
     * User currentUser = getCurrentUser();
     * Optional<Note> note = noteRepository.findByIdAndUser(noteId, currentUser);
     *
     * if (note.isPresent()) {
     *     // Note exists AND belongs to current user ✅
     *     return note.get();
     * } else {
     *     // Either note doesn't exist OR doesn't belong to user ❌
     *     throw new ForbiddenException("Access denied");
     * }
     *
     * Security Example:
     * Alice tries: findByIdAndUser(3, alice)
     * Note 3 belongs to Bob (user_id=2)
     * Result: Optional.empty() → Access denied ✅
     *
     * Prevents:
     * - Unauthorized data access
     * - Information leakage
     * - Privilege escalation
     *
     * Alternative (INSECURE):
     * Note note = noteRepository.findById(3); // Gets ANY note
     * if (note.getUser().getId() != currentUser.getId()) {
     *     // Check AFTER fetching - not ideal!
     * }
     *
     * Our way (SECURE):
     * Optional<Note> note = findByIdAndUser(3, currentUser);
     * // Database does the filtering - better!
     *
     * @param id - Note ID to find
     * @param user - User who must own the note
     * @return Optional<Note> - Note if exists and belongs to user
     */
    Optional<Note> findByIdAndUser(Long id, User user);

    /**
     * FIND NOTES BY TITLE AND USER (Search within user's notes)
     *
     * Search for notes by title, but only within current user's notes.
     *
     * Generated SQL:
     * SELECT * FROM notes
     * WHERE title ILIKE '%?%'
     * AND user_id = ?
     *
     * Why user-scoped search:
     * - Search should only return user's own notes
     * - Don't show other users' notes in search results
     * - Privacy and security
     *
     * Usage:
     * List<Note> results = noteRepository
     *     .findByTitleContainingIgnoreCaseAndUser("Java", currentUser);
     * // Only searches within current user's notes
     *
     * @param title - Search term
     * @param user - User whose notes to search
     * @return List<Note> - Matching notes belonging to user
     */
    List<Note> findByTitleContainingIgnoreCaseAndUser(String title, User user);

    /**
     * FIND NOTES BY CONTENT AND USER (Content search within user's notes)
     *
     * Search in note content, scoped to user's notes.
     *
     * Generated SQL:
     * SELECT * FROM notes
     * WHERE content ILIKE '%?%'
     * AND user_id = ?
     *
     * @param content - Search term
     * @param user - User whose notes to search
     * @return List<Note> - Matching notes belonging to user
     */
    List<Note> findByContentContainingIgnoreCaseAndUser(String content, User user);

    /*
     * ═══════════════════════════════════════════════════════════
     * WHY THESE METHODS ARE ESSENTIAL
     * ═══════════════════════════════════════════════════════════
     *
     * 1. DATA ISOLATION
     *    - Each user sees only their own data
     *    - Multi-tenant application pattern
     *    - Prevents data leakage
     *
     * 2. SECURITY BY DESIGN
     *    - Database enforces ownership
     *    - Cannot accidentally expose data
     *    - Authorization built into queries
     *
     * 3. PERFORMANCE
     *    - Queries filter by user_id (indexed)
     *    - Smaller result sets
     *    - Faster queries
     *
     * 4. SIMPLICITY
     *    - Repository handles filtering
     *    - Service layer cleaner
     *    - Less manual checking
     *
     * 5. CORRECTNESS
     *    - Impossible to return wrong data
     *    - Type-safe (User parameter)
     *    - Compile-time checking
     */

    /**
     * HOW SPRING DATA JPA WORKS:
     *
     * Method naming convention creates queries automatically!
     *
     * Pattern: findBy + FieldName + Condition
     *
     * Examples:
     * - findByTitle → WHERE title = ?
     * - findByTitleContaining → WHERE title LIKE %?%
     * - findByTitleIgnoreCase → WHERE LOWER(title) = LOWER(?)
     * - findByCreatedAtBefore → WHERE created_at < ?
     * - findByTitleAndContent → WHERE title = ? AND content = ?
     * - findByTitleOrContent → WHERE title = ? OR content = ?
     *
     * No SQL needed! Spring generates it!
     */
}