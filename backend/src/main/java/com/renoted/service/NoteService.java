package com.renoted.service;

import com.renoted.dto.NoteDTO;
import com.renoted.entity.Note;
import com.renoted.entity.Tag;
import com.renoted.repo.NoteRepo;
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

    @Autowired  // <-- Tells Spring to inject NoteRepo
    //     Spring looks for a bean of type NoteRepo
    //     Finds our interface (Spring created implementation)
    //     Injects it into this constructor
    //     This is CONSTRUCTOR INJECTION (best practice)

    public NoteService(NoteRepo noteRepo, TagService tagService) {
        this.noteRepo = noteRepo;
        this.tagService = tagService;
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
     * CREATE - Create a new note (with tags)
     *
     * Flow:
     * 1. Convert DTO to Entity
     * 2. Process tags (convert tag IDs to Tag entities)
     * 3. Associate tags with note
     * 4. Save note (cascades to tags if new)
     * 5. Convert back to DTO and return
     *
     * Tag handling:
     * - Frontend sends tag IDs: [1, 2, 3]
     * - We fetch Tag entities from database
     * - Associate them with note
     * - Save creates entries in note_tags join table
     *
     * @param noteDTO - Note data with tag IDs
     * @return Created note with ID, timestamps, and tag IDs
     */
    public NoteDTO createNote(NoteDTO noteDTO) {
        // Step 1: Convert DTO to Entity
        Note note = convertToEntity(noteDTO);

        // Step 2: Handle tags if provided
        if (noteDTO.getTagIds() != null && !noteDTO.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : noteDTO.getTagIds()) {
                // Fetch existing tag by ID
                Tag tag = tagService.getTagEntityById(tagId);
                tags.add(tag);
            }
            note.setTags(tags);
        }

        // Step 3: Save note
        // Cascade will handle saving relationships to note_tags table
        Note savedNote = noteRepo.save(note);

        // Step 4: Convert to DTO and return
        return convertToDTO(savedNote);
    }

    /**
     * READ - Get all notes
     *
     * @return List of all notes
     *
     * Flow:
     * 1. Get all entities from repository
     * 2. Convert each entity to DTO
     * 3. Return list of DTOs
     */
    public List<NoteDTO> getAllNotes() {
        // Step 1: Get all notes from database
        List<Note> notes = noteRepo.findAll();
        // SQL: SELECT * FROM notes ORDER BY id;

        // Step 2: Convert each Note entity to NoteDTO
        // Using Java 8 Stream API:
        return notes.stream()  // Create stream from list
                .map(this::convertToDTO)  // Convert each Note to NoteDTO
                .collect(Collectors.toList());  // Collect back to list

        // What stream().map().collect() does:
        // List<Note> notes = [note1, note2, note3]
        //   ↓ stream()
        // Stream<Note> = [note1, note2, note3]
        //   ↓ map(convertToDTO)
        // Stream<NoteDTO> = [dto1, dto2, dto3]
        //   ↓ collect(toList())
        // List<NoteDTO> = [dto1, dto2, dto3]

        // Without streams (traditional approach):
        // List<NoteDTO> dtos = new ArrayList<>();
        // for (Note note : notes) {
        //     dtos.add(convertToDTO(note));
        // }
        // return dtos;
    }

    /**
     * READ - Get single note by ID
     *
     * @param id - Note ID
     * @return Note if found
     * @throws RuntimeException if note not found
     *
     * Flow:
     * 1. Try to find note by ID
     * 2. If found: convert to DTO and return
     * 3. If not found: throw exception
     */
    public NoteDTO getNoteById(Long id) {
        // findById returns Optional<Note>
        // Optional = container that may or may not contain a value
        // Why Optional?
        // - Avoids null pointer exceptions
        // - Forces us to handle "not found" case
        // - Makes code more explicit

        Note note = noteRepo.findById(id)
                // If note exists, return it
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));
        // If note doesn't exist, throw exception
        // Controller will catch this and return 404 Not Found

        // SQL executed: SELECT * FROM notes WHERE id = ?;

        // Convert entity to DTO and return
        return convertToDTO(note);

        // What is Optional?
        // Optional<Note> optional = noteRepo.findById(1L);
        //
        // If note exists:
        //   optional.isPresent() → true
        //   optional.get() → returns the Note
        //   optional.orElseThrow() → returns the Note
        //
        // If note doesn't exist:
        //   optional.isPresent() → false
        //   optional.get() → throws NoSuchElementException
        //   optional.orElseThrow() → throws our custom exception
    }

    /**
     * UPDATE - Update existing note (including tags)
     *
     * Tag update strategy:
     * - Replace all tags with new set
     * - Frontend sends complete list of tag IDs
     * - We clear old tags and set new ones
     *
     * Example:
     * Old tags: [Java, Spring]
     * New tags: [Java, React] (user removed Spring, added React)
     * Result: [Java, React]
     *
     * @param id - Note ID
     * @param noteDTO - Updated note data
     * @return Updated note
     */
    public NoteDTO updateNote(Long id, NoteDTO noteDTO) {
        // Find existing note
        Note existingNote = noteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));

        // Update basic fields
        existingNote.setTitle(noteDTO.getTitle());

        // ⚠️ SECURITY: Sanitize HTML to prevent XSS
        String sanitizedContent = HtmlSanitizer.sanitize(noteDTO.getContent());
        existingNote.setContent(sanitizedContent);

        if (HtmlSanitizer.containsDangerousContent(noteDTO.getContent())) {
            System.out.println("🚨 XSS ATTACK BLOCKED on note update: " + noteDTO.getTitle());
        }

        // Update tags
        if (noteDTO.getTagIds() != null) {
            // Clear existing tags
            existingNote.getTags().clear();

            // Add new tags
            if (!noteDTO.getTagIds().isEmpty()) {
                Set<Tag> newTags = new HashSet<>();
                for (Long tagId : noteDTO.getTagIds()) {
                    // Fetch existing tag by ID
                    Tag tag = tagService.getTagEntityById(tagId);
                    newTags.add(tag);
                }
                existingNote.setTags(newTags);
            }
        }

        // Save updated note
        Note updatedNote = noteRepo.save(existingNote);
        return convertToDTO(updatedNote);
    }

    /**
     * DELETE - Delete note by ID
     *
     * @param id - Note ID to delete
     * @throws RuntimeException if note not found
     *
     * Flow:
     * 1. Check if note exists
     * 2. Delete it
     */
    public void deleteNote(Long id) {
        // Step 1: Check if note exists
        // Why check first?
        // - Give clear error message if not found
        // - Avoid silent failures
        if (!noteRepo.existsById(id)) {
            throw new RuntimeException("Note not found with id: " + id);
        }
        // SQL: SELECT EXISTS(SELECT 1 FROM notes WHERE id = ?);
        // Returns true/false

        // Step 2: Delete the note
        noteRepo.deleteById(id);
        // SQL: DELETE FROM notes WHERE id = ?;

        // Alternative approach (fetch then delete):
        // Note note = noteRepo.findById(id).orElseThrow(...);
        // noteRepo.delete(note);
        //
        // Our approach is more efficient:
        // - 1 SELECT (existsById) + 1 DELETE
        // vs
        // - 1 SELECT (findById) + 1 DELETE + fetching all data
    }

    /**
     * SEARCH - Search notes by keyword
     *
     * Searches in both title and content (case-insensitive)
     *
     * Examples:
     * - search("java") → Finds "Java Tutorial", "Spring and java"
     * - search("SPRING") → Finds "spring boot", "Spring Framework"
     *
     * @param query - Search keyword
     * @return List of matching notes
     */
    public List<NoteDTO> searchNotes(String query) {
        if (query == null || query.isBlank()) {
            // If no query, return all notes
            return getAllNotes();
        }

        // Trim query
        String trimmedQuery = query.trim();

        // Search in title and content
        List<Note> notes = noteRepo
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                        trimmedQuery,
                        trimmedQuery
                );

        // Convert to DTOs
        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * FILTER - Get notes by tag
     *
     * Returns all notes that have the specified tag
     *
     * Example:
     * - filterByTag(1) → All notes tagged with tag ID 1
     *
     * @param tagId - Tag ID to filter by
     * @return List of notes with this tag
     */
    public List<NoteDTO> filterByTag(Long tagId) {
        List<Note> notes = noteRepo.findByTagId(tagId);

        // Convert to DTOs
        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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