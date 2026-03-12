package com.renoted.service;

import com.renoted.dto.TagDTO;
import com.renoted.entity.Note;
import com.renoted.entity.Tag;
import com.renoted.repo.TagRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TagService - Business Logic Layer for Tags
 *
 * Purpose: Manages tag operations and conversions
 *
 * Responsibilities:
 * - CRUD operations for tags
 * - Entity ↔ DTO conversion
 * - Tag name normalization (Title Case)
 * - Duplicate prevention
 * - Business logic and validation
 */
@Service
@Transactional
public class TagService {

    private final TagRepo tagRepository;

    @Autowired
    public TagService(TagRepo tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * CREATE - Create a new tag
     *
     * Flow:
     * 1. Normalize tag name (Title Case)
     * 2. Check if tag already exists (case-insensitive)
     * 3. If exists, throw exception
     * 4. If not, create and save new tag
     * 5. Return DTO
     *
     * Why normalize to Title Case?
     * - Consistent display: "Java" not "java" or "JAVA"
     * - Better UX
     * - Easier to read
     *
     * Examples:
     * - "java" → "Java"
     * - "spring boot" → "Spring Boot"
     * - "REACT" → "React"
     *
     * @param tagDTO - Tag data from frontend
     * @return Created tag with ID
     * @throws RuntimeException if tag already exists
     */
    public TagDTO createTag(TagDTO tagDTO) {
        // Step 1: Normalize name to Title Case
        String normalizedName = normalizeName(tagDTO.getName());

        // Step 2: Check if tag already exists
        if (tagRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new RuntimeException("Tag with name '" + normalizedName + "' already exists");
        }

        // Step 3: Create new tag
        Tag tag = new Tag();
        tag.setName(normalizedName);

        // Step 4: Save to database
        Tag savedTag = tagRepository.save(tag);

        // Step 5: Convert to DTO and return
        return convertToDTO(savedTag);
    }

    /**
     * READ - Get all tags
     *
     * Returns all tags in alphabetical order
     *
     * @return List of all tags
     */
    public List<TagDTO> getAllTags() {
        List<Tag> tags = tagRepository.findAll();

        // Convert to DTOs and sort by name
        return tags.stream()
                .map(this::convertToDTO)
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .collect(Collectors.toList());

        // What this does:
        // 1. tags.stream() - Create stream from list
        // 2. .map(this::convertToDTO) - Convert each Tag to TagDTO
        // 3. .sorted(...) - Sort alphabetically (case-insensitive)
        // 4. .collect(toList()) - Collect back to list
    }

    /**
     * READ - Get single tag by ID
     *
     * @param id - Tag ID
     * @return Tag if found
     * @throws RuntimeException if tag not found
     */
    public TagDTO getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + id));

        return convertToDTO(tag);
    }

    /**
     * Get Tag entity by ID
     *
     * Internal use - returns Tag entity (not DTO)
     * Used by NoteService when associating tags with notes
     *
     * @param id - Tag ID
     * @return Tag entity
     * @throws RuntimeException if tag not found
     */
    public Tag getTagEntityById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + id));
    }

    /**
     * READ - Get or create tag by name
     *
     * Why we need this:
     * - When user creates note with tags: ["Java", "Spring"]
     * - If "Java" exists, use existing tag
     * - If "Java" doesn't exist, create it
     * - Prevents duplicate tags
     *
     * This is used internally by NoteService when saving notes with tags
     *
     * @param name - Tag name
     * @return Existing tag or newly created tag
     */
    public Tag getOrCreateTag(String name) {
        String normalizedName = normalizeName(name);

        // Try to find existing tag
        return tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    // Tag doesn't exist, create new one
                    Tag newTag = new Tag(normalizedName);
                    return tagRepository.save(newTag);
                });

        // orElseGet():
        // - If Optional has value (tag found), return it
        // - If Optional is empty (tag not found), execute lambda and return result
    }

    /**
     * DELETE - Delete tag by ID
     *
     * This method:
     * 1. Finds the tag
     * 2. Removes tag from all notes (clears associations in note_tags table)
     * 3. Deletes the tag from tags table
     * 4. Notes remain intact
     *
     * Example:
     * Before: Note1 has tags [Java, Spring], Note2 has tags [Java, React]
     * Delete "Java" tag
     * After: Note1 has tags [Spring], Note2 has tags [React]
     *
     * @param id - Tag ID to delete
     * @throws RuntimeException if tag not found
     */
    public void deleteTag(Long id) {
        // Step 1: Find the tag
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + id));

        // Step 2: Get all notes using this tag
        Set<Note> notesWithTag = tag.getNotes();

        // Step 3: Remove this tag from all notes
        // This updates the note_tags join table
        for (Note note : notesWithTag) {
            note.getTags().remove(tag);
            // Hibernate will execute:
            // DELETE FROM note_tags WHERE note_id = ? AND tag_id = ?
        }

        // Step 4: Clear the tag's note references (for clean deletion)
        tag.getNotes().clear();

        // Step 5: Delete the tag
        // Now safe to delete because all associations are removed
        tagRepository.deleteById(id);

        // What happens in database:
        // 1. DELETE FROM note_tags WHERE tag_id = ? (already done in step 3)
        // 2. DELETE FROM tags WHERE id = ?
        // Notes table: UNCHANGED ✅
    }

    /**
     * HELPER - Normalize tag name to Title Case
     *
     * Title Case Rules:
     * - First letter of each word capitalized
     * - Rest lowercase
     * - Trim whitespace
     *
     * Examples:
     * - "java" → "Java"
     * - "spring boot" → "Spring Boot"
     * - "REACT" → "React"
     * - "  frontend  " → "Frontend"
     *
     * @param name - Raw tag name from user
     * @return Normalized name in Title Case
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        // Trim whitespace
        name = name.trim();

        // Split by spaces (for multi-word tags like "Spring Boot")
        String[] words = name.split("\\s+");

        // Convert each word to Title Case
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                // First letter uppercase, rest lowercase
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        // Remove trailing space and return
        return result.toString().trim();

        // Examples of execution:
        // "spring boot" → ["spring", "boot"] → "Spring Boot"
        // "REACT" → ["REACT"] → "React"
        // "java" → ["java"] → "Java"
    }

    /**
     * HELPER - Convert Entity to DTO
     *
     * Important: We DON'T include the notes field
     * This prevents circular reference in JSON
     *
     * @param tag - Entity from database
     * @return DTO for API response
     */
    private TagDTO convertToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName()
        );

        // Note: We don't convert tag.getNotes()
        // This would cause infinite recursion:
        // Tag → Note → Tags → Note → Tags → ...
    }

    /**
     * HELPER - Convert DTO to Entity
     *
     * Used when creating new tags
     *
     * @param tagDTO - DTO from API request
     * @return Entity ready to save
     */
    private Tag convertToEntity(TagDTO tagDTO) {
        Tag tag = new Tag();
        if (tagDTO.getId() != null) {
            tag.setId(tagDTO.getId());
        }
        tag.setName(normalizeName(tagDTO.getName()));
        return tag;
    }

    /**
     * DESIGN NOTES:
     *
     * Why normalize names?
     * - Consistency: All tags look professional
     * - Prevents duplicates: "java", "Java", "JAVA" all become "Java"
     * - Better UX: Users see consistent tag names
     *
     * Why getOrCreateTag()?
     * - When creating notes with tags, we need to handle:
     *   1. Existing tags (reuse them)
     *   2. New tags (create them)
     * - This method does both
     * - Used by NoteService when saving notes
     *
     * Why not cascade delete notes when deleting tags?
     * - Tags are labels, not containers
     * - Deleting a label shouldn't delete the content
     * - Notes can exist without tags
     * - User can always re-tag notes
     */
}