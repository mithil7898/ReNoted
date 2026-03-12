package com.renoted.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Tag Entity
 *
 * Purpose: Represents a tag/label for categorizing notes
 *
 * Many-to-Many Relationship:
 * - One tag can be associated with many notes
 * - One note can have many tags
 *
 * Example:
 * Tag "Java" → Note1, Note2, Note5
 * Note1 → Tags: "Java", "Spring", "Backend"
 *
 * Database Structure:
 * 1. tags table (this entity)
 * 2. notes table (Note entity)
 * 3. note_tags table (join table, auto-created by Hibernate)
 */
@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "notes")  // ← Must have this line!
@ToString(exclude = "notes")            // ← Must have this line!
public class Tag {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tag Name
     *
     * - Unique: No duplicate tag names
     * - Not null: Required field
     * - Max 50 characters: Short, concise tags
     *
     * Examples: "Java", "Spring Boot", "Frontend", "Important"
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Many-to-Many Relationship with Note
     *
     * @ManyToMany: One tag can have many notes, one note can have many tags
     *
     * mappedBy = "tags":
     * - This is the inverse side of the relationship
     * - The Note entity owns the relationship
     * - Note entity has @ManyToMany(mappedBy) annotation
     * - This tells Hibernate: "Note entity controls this relationship"
     *
     * Why mappedBy?
     * - Prevents creating duplicate join tables
     * - Only one side (Note) controls the relationship
     * - Tag is just "aware" of the relationship
     *
     * Why Set instead of List?
     * - Set prevents duplicate associations
     * - More efficient for relationships
     * - No ordering needed for tags
     * - Hibernate recommends Set for Many-to-Many
     *
     * Why HashSet?
     * - Default implementation of Set
     * - Fast lookups (O(1) average)
     * - Prevents duplicates automatically
     *
     * Example:
     * Tag javaTag = new Tag("Java");
     * javaTag.getNotes() → [note1, note2, note5]
     */
    @ManyToMany(mappedBy = "tags")
    // mappedBy = "tags" refers to the "tags" field in Note entity
    private Set<Note> notes = new HashSet<>();
    // Initialize to empty set to avoid NullPointerException

    /**
     * Constructor for creating tag with just name
     * Useful for creating new tags
     */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * IMPORTANT: Lombok's @Data generates equals() and hashCode()
     * For entities with relationships, we should be careful!
     *
     * For Tag, we'll use name for equality:
     * - Two tags with same name = same tag
     * - This prevents duplicate tags in database (UNIQUE constraint helps)
     */
}