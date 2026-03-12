package com.renoted.controller;

import com.renoted.dto.TagDTO;
import com.renoted.service.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TagController - REST API Layer for Tags
 *
 * Purpose: Exposes HTTP endpoints for tag operations
 *
 * Base URL: /api/tags
 *
 * Endpoints:
 * - POST   /api/tags       - Create new tag
 * - GET    /api/tags       - Get all tags
 * - GET    /api/tags/{id}  - Get single tag
 * - DELETE /api/tags/{id}  - Delete tag
 *
 * Note: We don't have PUT (update) for tags
 * Why? Tags are simple labels - if you want different name, create new tag
 * Updating could cause confusion for notes already using the tag
 */
@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "http://localhost:5173")
public class TagController {

    private final TagService tagService;

    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * CREATE - Create a new tag
     *
     * POST /api/tags
     *
     * Request Body:
     * {
     *   "name": "Java"
     * }
     *
     * Response: 201 Created
     * {
     *   "id": 1,
     *   "name": "Java"
     * }
     *
     * Validation:
     * - Name is required (@NotBlank)
     * - Name length: 1-50 characters (@Size)
     * - Name is normalized to Title Case by service
     * - Duplicate names prevented (case-insensitive)
     *
     * Example scenarios:
     * 1. Create "java" → Normalized to "Java", saved, returns 201
     * 2. Create "Java" again → Error 400 "Tag already exists"
     * 3. Create "" (empty) → Error 400 "Tag name is required"
     * 4. Create "Very Long Tag Name That Exceeds Fifty Characters Limit"
     *    → Error 400 "Tag name must be between 1 and 50 characters"
     *
     * @param tagDTO - Tag data from frontend
     * @return Created tag with ID and normalized name
     */
    @PostMapping
    public ResponseEntity<TagDTO> createTag(@Valid @RequestBody TagDTO tagDTO) {
        // @Valid triggers validation annotations in TagDTO
        // If validation fails, Spring returns 400 Bad Request automatically
        // If validation passes, service handles business logic

        try {
            TagDTO createdTag = tagService.createTag(tagDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
            // 201 Created = Standard for successful POST
        } catch (RuntimeException e) {
            // Service throws exception if tag already exists
            // Return 400 Bad Request with error message
            // Note: In production, use proper exception handlers (@ControllerAdvice)
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * READ - Get all tags
     *
     * GET /api/tags
     *
     * Response: 200 OK
     * [
     *   { "id": 1, "name": "Java" },
     *   { "id": 2, "name": "Spring" },
     *   { "id": 3, "name": "React" }
     * ]
     *
     * Tags are returned in alphabetical order (handled by service)
     *
     * Use cases:
     * - Display all available tags in UI
     * - Show tag selector when creating/editing notes
     * - Filter notes by tag
     *
     * @return List of all tags, alphabetically sorted
     */
    @GetMapping
    public ResponseEntity<List<TagDTO>> getAllTags() {
        List<TagDTO> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
        // 200 OK with list of tags
        // If no tags exist, returns empty array []
    }

    /**
     * READ - Get single tag by ID
     *
     * GET /api/tags/{id}
     *
     * Example: GET /api/tags/1
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "name": "Java"
     * }
     *
     * Response if not found: 404 Not Found
     *
     * Use cases:
     * - Get details of specific tag
     * - Verify tag exists before associating with note
     *
     * @param id - Tag ID
     * @return Tag if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagDTO> getTagById(@PathVariable Long id) {
        try {
            TagDTO tag = tagService.getTagById(id);
            return ResponseEntity.ok(tag);
            // 200 OK with tag data
        } catch (RuntimeException e) {
            // Service throws exception if tag not found
            // Return 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE - Delete tag by ID
     *
     * DELETE /api/tags/{id}
     *
     * Example: DELETE /api/tags/1
     *
     * Response: 204 No Content (success, no body)
     *
     * What happens when you delete a tag:
     * 1. Tag is removed from tags table
     * 2. Associations removed from note_tags join table
     * 3. Notes remain intact (just lose the tag)
     *
     * Example:
     * Before: Note1 has tags [Java, Spring, React]
     * Delete "Spring" tag
     * After: Note1 has tags [Java, React]
     *
     * Use cases:
     * - Remove unused tags
     * - Clean up misspelled tags
     * - Consolidate tags (delete duplicates)
     *
     * Note: Deleting a tag does NOT delete notes with that tag
     *
     * @param id - Tag ID to delete
     * @return 204 No Content on success, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        try {
            tagService.deleteTag(id);
            return ResponseEntity.noContent().build();
            // 204 No Content = Success, no response body needed
        } catch (RuntimeException e) {
            // Service throws exception if tag not found
            // Return 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DESIGN DECISIONS:
     *
     * Why no PUT (update) endpoint?
     * - Tags are simple labels, not complex entities
     * - If user wants different name, create new tag and re-tag notes
     * - Updating tag name could confuse users who already used it
     * - Keep it simple: Create and Delete only
     *
     * Why 201 Created for POST?
     * - Standard HTTP response for successful resource creation
     * - Indicates new resource was created
     * - Client can use returned data (with ID) immediately
     *
     * Why 204 No Content for DELETE?
     * - Standard for successful DELETE
     * - No body needed (deletion confirmed by status code)
     * - More efficient than returning deleted object
     *
     * Exception handling:
     * - Currently using try-catch in each method
     * - In production, use @ControllerAdvice for global exception handling
     * - Would convert RuntimeException to appropriate HTTP responses
     * - Provides consistent error format across all endpoints
     *
     * Example with @ControllerAdvice (future improvement):
     *
     * @ControllerAdvice
     * public class GlobalExceptionHandler {
     *     @ExceptionHandler(RuntimeException.class)
     *     public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
     *         ErrorResponse error = new ErrorResponse(
     *             HttpStatus.BAD_REQUEST.value(),
     *             e.getMessage(),
     *             LocalDateTime.now()
     *         );
     *         return ResponseEntity.badRequest().body(error);
     *     }
     * }
     */
}