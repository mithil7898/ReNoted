package com.renoted.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Note Entity
 *
 * Purpose: Represents a note in the database
 * This is a JPA entity - it maps to a database table
 *
 * What is an Entity?
 * - A Java class that represents a database table
 * - Each instance = one row in the table
 * - Each field = one column in the table
 *
 * Why we use JPA annotations:
 * - Tell Hibernate how to map this class to database
 * - Hibernate generates SQL automatically
 * - We don't write CREATE TABLE, INSERT, UPDATE, DELETE queries!
 *
 * Example:
 * Java:     Note note = new Note("My Title", "My Content");
 * Database: INSERT INTO notes (title, content) VALUES ('My Title', 'My Content');
 */
@Entity  // <-- Tells JPA: "This class is a database table"
//     Hibernate will create a table named "note" (lowercase class name)
//     We can customize with @Table(name = "notes") if we want

@Table(name = "notes")  // <-- Optional: Specify exact table name
//     Without this, table would be called "note"
//     With this, table is called "notes" (clearer)

@Data  // <-- Lombok annotation: Generates getters, setters, toString, equals, hashCode
//     Without this, we'd need to write 50+ lines of boilerplate code!
//     Equivalent to:
//     - public String getTitle() { return title; }
//     - public void setTitle(String title) { this.title = title; }
//     - public String toString() { ... }
//     - public boolean equals(Object o) { ... }
//     - public int hashCode() { ... }

@NoArgsConstructor  // <-- Lombok: Generates empty constructor
//     Note() { }
//     Required by JPA/Hibernate

@AllArgsConstructor  // <-- Lombok: Generates constructor with all fields
//     Note(Long id, String title, String content, ...) { ... }
//     Useful for creating test objects

@EqualsAndHashCode(exclude = "tags")  // ← Must have this line!
@ToString(exclude = "tags")            // ← Must have this line!
public class Note {

    /**
     * Primary Key (ID)
     *
     * Every database table needs a primary key
     * This uniquely identifies each note
     */
    @Id  // <-- Marks this field as the primary key
    //     Required for every entity

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // <-- Auto-generate ID values
    //     IDENTITY = Use database auto-increment
    //     PostgreSQL will automatically assign: 1, 2, 3, 4...
    //     We never set ID manually - database does it

    @Column(name = "id")  // <-- Optional: Specify column name in database
    //     Without this, column would still be "id"
    //     This is just explicit

    private Long id;
    // Why Long and not long?
    // - Long can be null (before saving to database, ID doesn't exist)
    // - long is primitive, cannot be null
    // - New notes: id = null → Database assigns ID → id = 1, 2, 3...

    /**
     * Note Title
     *
     * Required field, max 255 characters
     */
    @Column(name = "title", nullable = false, length = 255)
    // nullable = false → NOT NULL in database (required field)
    // length = 255 → VARCHAR(255) in database
    // This enforces database constraints!

    private String title;

    /**
     * Note Content
     *
     * Optional field, can be very long
     * Uses TEXT type in database (unlimited length)
     */
    @Column(name = "content", columnDefinition = "TEXT")
    // columnDefinition = "TEXT" → Use TEXT type (not VARCHAR)
    // TEXT can store very long content (65,535 characters in PostgreSQL)
    // VARCHAR is limited to 255 by default

    private String content;

    // ⭐ NEW: User relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Creation Timestamp
     *
     * Automatically set when note is first saved
     * Never changes after creation
     */
    @CreationTimestamp  // <-- Hibernate annotation: Set timestamp on INSERT
    //     Automatically fills this field when saving new note
    //     We NEVER set this manually!

    @Column(name = "created_at", nullable = false, updatable = false)
    // updatable = false → This field NEVER changes after creation
    // nullable = false → Always has a value

    private LocalDateTime createdAt;
    // LocalDateTime = Java 8 date/time (no timezone)
    // Example: 2024-03-02T15:30:45

    /**
     * Last Update Timestamp
     *
     * Automatically updated every time note is modified
     */
    @UpdateTimestamp  // <-- Hibernate annotation: Update timestamp on INSERT and UPDATE
    //     Automatically updates this field when note is saved
    //     We NEVER set this manually!

    @Column(name = "updated_at", nullable = false)

    private LocalDateTime updatedAt;
    // Gets set on creation AND every update
    // Example usage:
    // - Note created: createdAt = 15:30, updatedAt = 15:30
    // - Note updated: createdAt = 15:30, updatedAt = 16:45 (changed!)


    /**
     * Many-to-Many Relationship with Tag
     *
     * @ManyToMany: One note can have many tags
     *
     * @JoinTable: Specifies the join table configuration
     * - name = "note_tags": Name of the join table Hibernate will create
     * - joinColumns: Foreign key to THIS entity (Note)
     * - inverseJoinColumns: Foreign key to OTHER entity (Tag)
     *
     * Join Table Structure:
     * note_tags (
     *   note_id BIGINT REFERENCES notes(id),
     *   tag_id BIGINT REFERENCES tags(id),
     *   PRIMARY KEY (note_id, tag_id)
     * )
     *
     * Why @JoinTable here (in Note) and not in Tag?
     * - One side must "own" the relationship
     * - We chose Note to be the owner
     * - Owner side controls the join table
     * - Tag side uses mappedBy to refer to this
     *
     * cascade = CascadeType.PERSIST:
     * - When saving a note with new tags, tags are saved automatically
     * - Example: note.getTags().add(newTag); noteRepository.save(note);
     *   → Both note AND newTag are saved
     *
     * We DON'T use CascadeType.REMOVE because:
     * - Deleting a note shouldn't delete its tags
     * - Tags can be shared across multiple notes
     * - Only delete the association, not the tag itself
     *
     * Why Set instead of List?
     * - No duplicate tags on same note
     * - More efficient for contains() checks
     * - Hibernate recommendation for Many-to-Many
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "note_tags",  // Name of join table
            joinColumns = @JoinColumn(name = "note_id"),  // Foreign key to Note
            inverseJoinColumns = @JoinColumn(name = "tag_id")  // Foreign key to Tag
    )
    private Set<Tag> tags = new HashSet<>();
}