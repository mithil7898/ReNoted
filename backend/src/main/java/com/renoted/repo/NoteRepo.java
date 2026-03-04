package com.renoted.repo;

import com.renoted.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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