package com.renoted.repo;

import com.renoted.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TagRepository - Database Access Layer for Tags
 *
 * Purpose: Handles database operations for Tag entity
 * Extends JpaRepository to get automatic CRUD methods
 *
 * What we get for FREE from JpaRepository<Tag, Long>:
 * - save(tag) → INSERT or UPDATE
 * - findById(id) → SELECT by ID
 * - findAll() → SELECT all tags
 * - deleteById(id) → DELETE by ID
 * - count() → COUNT tags
 * - existsById(id) → Check if exists
 */
@Repository
public interface TagRepo extends JpaRepository<Tag, Long> {

    /**
     * Find tag by name (case-insensitive)
     *
     * Why we need this:
     * - Prevent duplicate tags with different cases
     * - Example: "Java" and "java" should be the same tag
     *
     * Method Naming Convention:
     * - findBy = SELECT query
     * - Name = field name in Tag entity
     * - IgnoreCase = case-insensitive comparison
     *
     * Spring Data JPA generates this SQL:
     * SELECT * FROM tags WHERE LOWER(name) = LOWER(?)
     *
     * Returns Optional<Tag>:
     * - Optional.of(tag) if found
     * - Optional.empty() if not found
     *
     * Usage:
     * Optional<Tag> tag = tagRepository.findByNameIgnoreCase("java");
     * if (tag.isPresent()) {
     *     // Tag exists, use tag.get()
     * } else {
     *     // Tag doesn't exist, create new one
     * }
     *
     * @param name - Tag name to search for
     * @return Optional containing tag if found, empty otherwise
     */
    Optional<Tag> findByNameIgnoreCase(String name);

    /**
     * Check if tag exists by name (case-insensitive)
     *
     * Why we need this:
     * - Quick check before creating new tag
     * - More efficient than findByNameIgnoreCase
     * - Returns boolean instead of fetching entire entity
     *
     * Spring Data JPA generates:
     * SELECT EXISTS(SELECT 1 FROM tags WHERE LOWER(name) = LOWER(?))
     *
     * This is more efficient than:
     * - findByNameIgnoreCase().isPresent()
     * - Because it doesn't fetch the entity, just checks existence
     *
     * Usage:
     * if (tagRepository.existsByNameIgnoreCase("java")) {
     *     throw new Exception("Tag already exists");
     * }
     *
     * @param name - Tag name to check
     * @return true if tag exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * OPTIONAL: Custom query examples for future use
     *
     * These are commented out but show advanced capabilities
     */

    /*
     * Find tags used by a specific note
     *
     * @Query annotation for custom JPQL
     * JPQL = Java Persistence Query Language (like SQL but for entities)
     *
     * The query:
     * - SELECT t FROM Tag t: Get tags
     * - JOIN t.notes n: Join with notes through Many-to-Many relationship
     * - WHERE n.id = :noteId: Filter by specific note ID
     *
     * Spring generates SQL:
     * SELECT t.* FROM tags t
     * INNER JOIN note_tags nt ON t.id = nt.tag_id
     * WHERE nt.note_id = ?
     */
    // @Query("SELECT t FROM Tag t JOIN t.notes n WHERE n.id = :noteId")
    // List<Tag> findTagsByNoteId(@Param("noteId") Long noteId);

    /*
     * Find tags with at least one note
     *
     * This finds tags that are actually being used
     * Useful for cleanup: delete unused tags
     */
    // @Query("SELECT DISTINCT t FROM Tag t WHERE SIZE(t.notes) > 0")
    // List<Tag> findTagsInUse();

    /*
     * Count notes using this tag
     *
     * Useful for displaying "Java (15 notes)" in UI
     */
    // @Query("SELECT COUNT(n) FROM Note n JOIN n.tags t WHERE t.id = :tagId")
    // long countNotesByTagId(@Param("tagId") Long tagId);

    /**
     * METHOD NAMING PATTERNS (Spring Data JPA Magic)
     *
     * Pattern: [verb][distinct?][By][field][condition]
     *
     * Examples:
     *
     * FIND queries:
     * - findByName → WHERE name = ?
     * - findByNameIgnoreCase → WHERE LOWER(name) = LOWER(?)
     * - findByNameContaining → WHERE name LIKE %?%
     * - findByNameStartingWith → WHERE name LIKE ?%
     * - findByNameEndingWith → WHERE name LIKE %?
     *
     * EXISTS queries:
     * - existsByName → SELECT EXISTS(SELECT 1 WHERE name = ?)
     * - existsByNameIgnoreCase → SELECT EXISTS(SELECT 1 WHERE LOWER(name) = LOWER(?))
     *
     * COUNT queries:
     * - countByName → SELECT COUNT(*) WHERE name = ?
     *
     * DELETE queries:
     * - deleteByName → DELETE WHERE name = ?
     *
     * ORDERING:
     * - findAllByOrderByNameAsc → ORDER BY name ASC
     * - findByNameContainingOrderByNameDesc → WHERE name LIKE %?% ORDER BY name DESC
     *
     * COMBINING conditions:
     * - findByNameAndActive → WHERE name = ? AND active = ?
     * - findByNameOrDescription → WHERE name = ? OR description = ?
     *
     * Spring Data JPA automatically generates SQL for ALL of these!
     * No need to write SQL manually!
     */
}