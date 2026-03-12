package com.renoted.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NoteDTO - Data Transfer Object
 *
 * Purpose: Represents note data sent to/from the API
 * This is what frontend sends and receives
 *
 * What is a DTO?
 * - Data Transfer Object
 * - Used to transfer data between layers (frontend ↔ backend)
 * - Contains only the data needed for API communication
 * - Does NOT have JPA annotations (not a database entity)
 *
 * Why use DTO instead of Entity?
 *
 * 1. SECURITY
 *    - Entity might have sensitive fields (passwords, internal IDs)
 *    - DTO exposes only what frontend needs
 *
 * 2. DECOUPLING
 *    - Can change database structure without breaking API
 *    - Entity changes don't affect API contract
 *    - Frontend doesn't know about database structure
 *
 * 3. VALIDATION
 *    - DTO can have validation rules (@NotBlank, @Size, etc.)
 *    - Validate input before touching database
 *
 * 4. CLEAN API
 *    - No Hibernate internal fields in JSON
 *    - No circular references
 *    - Clean, predictable JSON structure
 *
 * 5. FLEXIBILITY
 *    - Can combine multiple entities into one DTO
 *    - Can exclude fields from API
 *    - Can rename fields for frontend
 *
 * Example API Request (Create Note):
 * POST /api/notes
 * {
 *   "title": "My First Note",
 *   "content": "This is the content"
 * }
 *
 * Example API Response (Get Note):
 * GET /api/notes/1
 * {
 *   "id": 1,
 *   "title": "My First Note",
 *   "content": "This is the content",
 *   "createdAt": "2024-03-02T15:30:45",
 *   "updatedAt": "2024-03-02T15:30:45"
 * }
 */
@Data  // <-- Lombok: Generates getters, setters, toString, equals, hashCode
//     Same as we used in Entity
//     Makes this a clean POJO (Plain Old Java Object)

@NoArgsConstructor  // <-- Lombok: Generates empty constructor
//     Required for JSON deserialization
//     Jackson (JSON library) needs this to create objects

//@AllArgsConstructor  // <-- Lombok: Generates constructor with all fields
//     Useful for creating DTOs in code
//     Example: new NoteDTO(1L, "Title", "Content", ...)

public class NoteDTO {

    /**
     * Note ID
     *
     * - Included in responses (GET requests)
     * - NOT included in create requests (database generates it)
     * - Optional in update requests (taken from URL path)
     */
    private Long id;
    // When creating: frontend doesn't send this (null)
    // When reading: backend sends this (1, 2, 3...)
    // When updating: frontend may or may not send this

    /**
     * Note Title
     *
     * - Required field
     * - Must not be blank (not null, not empty, not just whitespace)
     * - Maximum 255 characters
     */
    @NotBlank(message = "Title is required")
    // @NotBlank = Must have content (not null, not "", not "   ")
    // Combines three checks:
    //   - @NotNull: title != null
    //   - @NotEmpty: title != ""
    //   - @NotBlank: title.trim() != ""
    // Example:
    //   ✅ "My Title" → Valid
    //   ❌ null → Invalid ("Title is required")
    //   ❌ "" → Invalid ("Title is required")
    //   ❌ "   " → Invalid ("Title is required")

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    // @Size = Check string length
    // min = 1: At least 1 character
    // max = 255: No more than 255 characters
    // Matches database VARCHAR(255) constraint
    // Example:
    //   ✅ "A" → Valid (1 char)
    //   ✅ "My Note Title" → Valid
    //   ❌ "" → Invalid (0 chars)
    //   ❌ "Very long title..." (256 chars) → Invalid

    private String title;

    /**
     * Note Content
     *
     * - Optional field (can be null or empty)
     * - No length limit (matches TEXT type in database)
     */
    // No validation annotations = optional field
    // Frontend can send:
    //   - "Some content" → ✅ Valid
    //   - "" → ✅ Valid (empty note)
    //   - null → ✅ Valid (no content yet)
    private String content;

    /**
     * Creation Timestamp
     *
     * - Set by backend (database)
     * - Never sent by frontend
     * - Read-only for API consumers
     */
    private LocalDateTime createdAt;
    // Frontend never sends this
    // Backend always sends this in responses
    // Example: "2024-03-02T15:30:45"

    /**
     * Last Update Timestamp
     *
     * - Updated by backend (database)
     * - Never sent by frontend
     * - Read-only for API consumers
     */
    private LocalDateTime updatedAt;
    // Frontend never sends this
    // Backend always sends this in responses
    // Example: "2024-03-02T16:45:30"

    /**
     * VALIDATION ANNOTATIONS WE USE:
     *
     * @NotNull - Value cannot be null
     *   Example: @NotNull Integer age;
     *   ✅ age = 25
     *   ❌ age = null
     *
     * @NotEmpty - Collection/String cannot be null or empty
     *   Example: @NotEmpty String name;
     *   ✅ name = "John"
     *   ❌ name = null
     *   ❌ name = ""
     *
     * @NotBlank - String cannot be null, empty, or whitespace
     *   Example: @NotBlank String title;
     *   ✅ title = "My Title"
     *   ❌ title = null
     *   ❌ title = ""
     *   ❌ title = "   "
     *
     * @Size - Check collection/string size
     *   Example: @Size(min=2, max=50) String username;
     *   ✅ username = "john" (4 chars)
     *   ❌ username = "a" (1 char, less than min)
     *   ❌ username = "verylongusername..." (51 chars, more than max)
     *
     * @Min / @Max - Numeric range
     *   Example: @Min(18) @Max(120) Integer age;
     *   ✅ age = 25
     *   ❌ age = 15 (less than 18)
     *   ❌ age = 150 (more than 120)
     *
     * @Email - Valid email format
     *   Example: @Email String email;
     *   ✅ email = "user@example.com"
     *   ❌ email = "notanemail"
     *
     * @Pattern - Regex pattern matching
     *   Example: @Pattern(regexp = "^[0-9]{10}$") String phone;
     *   ✅ phone = "1234567890"
     *   ❌ phone = "123" (too short)
     */

    /**
     * Tags associated with this note
     *
     * - List of tag IDs for API requests (create/update)
     * - Frontend sends: [1, 2, 3] (tag IDs)
     * - Backend converts to Tag entities
     *
     * Why List<Long> instead of List<TagDTO>?
     * - Simpler for frontend: just send tag IDs
     * - Frontend: { "title": "Note", "tagIds": [1, 2, 3] }
     * - Backend converts IDs to Tag entities internally
     *
     * Alternative approach (more verbose):
     * - Use List<TagDTO> with full tag objects
     * - Frontend: { "title": "Note", "tags": [{"id":1,"name":"Java"}] }
     * - More data to send, but more explicit
     *
     * We chose List<Long> for simplicity
     */
    private List<Long> tagIds;
    // When creating note: [1, 2] → Backend finds tags with these IDs
    // When returning note: [1, 2] → Frontend can display tag names


    public NoteDTO(Long id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public NoteDTO(Long id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt, List<Long> tagIds) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tagIds = tagIds;
    }
}