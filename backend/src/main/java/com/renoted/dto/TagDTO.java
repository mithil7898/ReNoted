package com.renoted.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TagDTO - Data Transfer Object for Tag
 *
 * Purpose: Transfer tag data between frontend and backend
 *
 * Why separate from Tag entity?
 * - Entity has bidirectional relationship with Note (notes field)
 * - Exposing entity directly causes infinite JSON recursion:
 *   Tag → Note → Tag → Note → ...
 * - DTO prevents this by only including necessary fields
 * - Clean API contract
 *
 * Fields:
 * - id: Tag identifier (null for new tags)
 * - name: Tag name (required, unique, max 50 chars)
 *
 * We DON'T include:
 * - notes field (Set<Note>) - prevents circular reference
 * - Would cause JSON serialization issues
 *
 * Example API request (Create tag):
 * POST /api/tags
 * { "name": "Java" }
 *
 * Example API response (Get tag):
 * GET /api/tags/1
 * { "id": 1, "name": "Java" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

    /**
     * Tag ID
     *
     * - Null when creating new tag
     * - Set by database after creation
     */
    private Long id;

    /**
     * Tag Name
     *
     * Validation:
     * - @NotBlank: Cannot be null, empty, or just whitespace
     * - @Size: Must be 1-50 characters
     *
     * Why 50 chars max?
     * - Tags should be short, concise labels
     * - Matches database VARCHAR(50) constraint
     * - Examples: "Java", "Spring Boot", "Frontend"
     *
     * Case handling:
     * - We'll normalize to Title Case in service layer
     * - "java" → "Java"
     * - "spring boot" → "Spring Boot"
     * - This ensures consistent tag names
     */
    @NotBlank(message = "Tag name is required")
    @Size(min = 1, max = 50, message = "Tag name must be between 1 and 50 characters")
    private String name;

    /**
     * Constructor with just name
     * Useful for creating new tags
     */
    public TagDTO(String name) {
        this.name = name;
    }
}