package com.renoted.controller;

import com.renoted.dto.NoteDTO;
import com.renoted.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NoteController - REST API Layer
 *
 * Purpose: Exposes HTTP endpoints for note operations
 * This is the entry point for all frontend requests
 *
 * What is a Controller?
 * - Handles HTTP requests (GET, POST, PUT, DELETE)
 * - Maps URLs to Java methods
 * - Validates input automatically
 * - Returns JSON responses automatically
 * - Delegates business logic to Service layer
 *
 * REST API Design:
 * - REST = Representational State Transfer
 * - Uses HTTP methods to indicate actions:
 *   - GET = Read (retrieve data)
 *   - POST = Create (new data)
 *   - PUT = Update (modify existing data)
 *   - DELETE = Delete (remove data)
 *
 * URL Structure:
 * - /api/notes → Collection (all notes)
 * - /api/notes/{id} → Single resource (specific note)
 *
 * Example Requests:
 *
 * Create Note:
 *   POST /api/notes
 *   Body: { "title": "My Note", "content": "Content" }
 *   Returns: { "id": 1, "title": "My Note", ... }
 *
 * Get All Notes:
 *   GET /api/notes
 *   Returns: [{ "id": 1, ... }, { "id": 2, ... }]
 *
 * Get Single Note:
 *   GET /api/notes/1
 *   Returns: { "id": 1, "title": "My Note", ... }
 *
 * Update Note:
 *   PUT /api/notes/1
 *   Body: { "title": "Updated", "content": "New content" }
 *   Returns: { "id": 1, "title": "Updated", ... }
 *
 * Delete Note:
 *   DELETE /api/notes/1
 *   Returns: 204 No Content
 */
@RestController  // <-- Combines @Controller + @ResponseBody
//     @Controller = This is a Spring MVC controller
//     @ResponseBody = Return values are automatically converted to JSON
//     Every method returns JSON (not HTML views)

@RequestMapping("/api/notes")  // <-- Base URL for all endpoints in this controller
//     All methods here start with /api/notes
//     Example: @GetMapping("/") → /api/notes/
//              @GetMapping("/{id}") → /api/notes/{id}

@CrossOrigin(origins = "http://localhost:5173")  // <-- Allow React frontend to call this API
//     Without this, browser blocks requests (CORS error)
//     Alternative: Global CORS config (we already have CorsConfig.java)
//     This is redundant with CorsConfig but doesn't hurt

public class NoteController {

    /**
     * Dependency Injection
     *
     * We need NoteService to handle business logic
     * Spring will automatically inject it
     */
    private final NoteService noteService;

    @Autowired  // <-- Spring injects NoteService
    //     Constructor injection (best practice)
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * CREATE - Create a new note
     *
     * URL: POST /api/notes
     * Request Body: { "title": "My Note", "content": "Content" }
     * Response: 201 Created + { "id": 1, "title": "My Note", ... }
     *
     * HTTP Status Codes:
     * - 201 Created = Resource successfully created
     * - 400 Bad Request = Validation failed (title missing, etc.)
     * - 500 Internal Server Error = Something went wrong
     */
    @PostMapping  // <-- Maps HTTP POST requests to this method
    //     Full URL: POST /api/notes
    //     No path specified = base URL (/api/notes)

    public ResponseEntity<NoteDTO> createNote(
            @Valid @RequestBody NoteDTO noteDTO
            // @RequestBody = Get data from HTTP request body (JSON)
            //               Spring automatically converts JSON to NoteDTO
            //               Example JSON: { "title": "My Note", "content": "..." }
            //               Becomes: NoteDTO object
            //
            // @Valid = Validate the DTO using annotations
            //         Checks @NotBlank, @Size, etc.
            //         If validation fails:
            //         - Returns 400 Bad Request automatically
            //         - Doesn't execute method body
            //         - Returns error message: "Title is required"
    ) {
        // Delegate to service layer
        // Service handles: conversion, saving, returning DTO
        NoteDTO createdNote = noteService.createNote(noteDTO);

        // Return ResponseEntity with:
        // - Status: 201 CREATED (standard for successful POST)
        // - Body: Created note with ID and timestamps
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);

        // Alternative shorthand:
        // return new ResponseEntity<>(createdNote, HttpStatus.CREATED);

        // What Spring does automatically:
        // 1. Receives HTTP POST request
        // 2. Reads JSON from request body
        // 3. Converts JSON to NoteDTO (using Jackson library)
        // 4. Validates NoteDTO (@Valid triggers validation)
        // 5. Calls this method with validated NoteDTO
        // 6. Method returns ResponseEntity<NoteDTO>
        // 7. Converts NoteDTO back to JSON
        // 8. Sends HTTP response with status 201 and JSON body
    }

    /**
     * READ - Get all notes
     *
     * URL: GET /api/notes
     * Response: 200 OK + [{ "id": 1, ... }, { "id": 2, ... }]
     *
     * HTTP Status Codes:
     * - 200 OK = Request successful
     * - 500 Internal Server Error = Something went wrong
     */
    @GetMapping  // <-- Maps HTTP GET requests to this method
    //     Full URL: GET /api/notes

    public ResponseEntity<List<NoteDTO>> getAllNotes() {
        // Get all notes from service
        List<NoteDTO> notes = noteService.getAllNotes();

        // Return 200 OK with list of notes
        return ResponseEntity.ok(notes);
        // ResponseEntity.ok() is shorthand for:
        // ResponseEntity.status(HttpStatus.OK).body(notes)

        // Example response:
        // Status: 200 OK
        // Body: [
        //   { "id": 1, "title": "First Note", "content": "...", ... },
        //   { "id": 2, "title": "Second Note", "content": "...", ... }
        // ]

        // If no notes exist:
        // Status: 200 OK
        // Body: []  (empty array)
    }

    /**
     * READ - Get single note by ID
     *
     * URL: GET /api/notes/{id}
     * Example: GET /api/notes/1
     * Response: 200 OK + { "id": 1, "title": "My Note", ... }
     *
     * HTTP Status Codes:
     * - 200 OK = Note found and returned
     * - 404 Not Found = Note with this ID doesn't exist
     * - 500 Internal Server Error = Something went wrong
     */
    @GetMapping("/{id}")  // <-- Maps GET requests with ID parameter
    //     {id} = Path variable (dynamic part of URL)
    //     Full URL: GET /api/notes/1
    //     {id} captures "1" from URL

    public ResponseEntity<NoteDTO> getNoteById(
            @PathVariable Long id
            // @PathVariable = Extract {id} from URL path
            //                URL: /api/notes/1 → id = 1L
            //                URL: /api/notes/42 → id = 42L
            //                URL: /api/notes/abc → Error (not a number)
    ) {
        // Get note from service
        // If note doesn't exist, service throws RuntimeException
        // We'll handle this with exception handler later
        NoteDTO note = noteService.getNoteById(id);

        // Return 200 OK with note data
        return ResponseEntity.ok(note);

        // What happens if note not found?
        // 1. noteService.getNoteById(999) throws RuntimeException("Note not found")
        // 2. Exception propagates to Spring
        // 3. Spring returns 500 Internal Server Error (not ideal)
        // 4. Better: Add @ExceptionHandler (we'll do this later)
        //    Then: Returns 404 Not Found with custom message
    }

    /**
     * UPDATE - Update existing note
     *
     * URL: PUT /api/notes/{id}
     * Example: PUT /api/notes/1
     * Request Body: { "title": "Updated Title", "content": "Updated content" }
     * Response: 200 OK + { "id": 1, "title": "Updated Title", ... }
     *
     * HTTP Status Codes:
     * - 200 OK = Note updated successfully
     * - 400 Bad Request = Validation failed
     * - 404 Not Found = Note with this ID doesn't exist
     * - 500 Internal Server Error = Something went wrong
     */
    @PutMapping("/{id}")  // <-- Maps HTTP PUT requests with ID
    //     PUT = Update existing resource
    //     Full URL: PUT /api/notes/1

    public ResponseEntity<NoteDTO> updateNote(
            @PathVariable Long id,  // ID from URL path
            @Valid @RequestBody NoteDTO noteDTO  // Updated data from request body
    ) {
        // Update note through service
        // Service will:
        // 1. Find existing note by ID (throws exception if not found)
        // 2. Update its fields
        // 3. Save to database
        // 4. Return updated DTO
        NoteDTO updatedNote = noteService.updateNote(id, noteDTO);

        // Return 200 OK with updated note
        return ResponseEntity.ok(updatedNote);

        // Example request:
        // PUT /api/notes/1
        // Body: { "title": "Updated", "content": "New content" }
        //
        // Example response:
        // Status: 200 OK
        // Body: {
        //   "id": 1,
        //   "title": "Updated",
        //   "content": "New content",
        //   "createdAt": "2024-03-02T15:30:45",
        //   "updatedAt": "2024-03-02T16:45:30"  ← Changed!
        // }
    }

    /**
     * DELETE - Delete note by ID
     *
     * URL: DELETE /api/notes/{id}
     * Example: DELETE /api/notes/1
     * Response: 204 No Content (no body)
     *
     * HTTP Status Codes:
     * - 204 No Content = Note deleted successfully (no response body)
     * - 404 Not Found = Note with this ID doesn't exist
     * - 500 Internal Server Error = Something went wrong
     */
    @DeleteMapping("/{id}")  // <-- Maps HTTP DELETE requests with ID
    //     DELETE = Remove resource
    //     Full URL: DELETE /api/notes/1

    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id
    ) {
        // Delete note through service
        // Service will:
        // 1. Check if note exists (throws exception if not found)
        // 2. Delete from database
        noteService.deleteNote(id);

        // Return 204 No Content
        // 204 = Success, but no response body needed
        // For DELETE, we don't need to return the deleted note
        return ResponseEntity.noContent().build();

        // Alternative status codes for DELETE:
        // - 204 No Content (most common, what we use)
        // - 200 OK + deleted object (if you want to return it)
        // - 202 Accepted (if deletion is asynchronous)

        // ResponseEntity<Void> = No response body
        // .noContent() = Sets status to 204
        // .build() = Constructs the ResponseEntity
    }

    /**
     * BONUS: Search notes by title (example custom endpoint)
     *
     * URL: GET /api/notes/search?title=keyword
     * Example: GET /api/notes/search?title=meeting
     * Response: 200 OK + [{ "id": 1, "title": "Meeting notes", ... }]
     *
     * This is commented out for now (we'll add search later)
     */
    /*
    @GetMapping("/search")
    public ResponseEntity<List<NoteDTO>> searchNotes(
            @RequestParam String title
            // @RequestParam = Extract query parameter from URL
            //                URL: /api/notes/search?title=meeting
            //                title = "meeting"
    ) {
        List<NoteDTO> notes = noteService.searchNotesByTitle(title);
        return ResponseEntity.ok(notes);
    }
    */

    /**
     * HTTP METHODS SUMMARY:
     *
     * GET = Read (retrieve data)
     *   - Safe: Doesn't modify data
     *   - Idempotent: Multiple calls return same result
     *   - Cacheable
     *   - Example: Get all notes, Get note by ID
     *
     * POST = Create (new resource)
     *   - Not safe: Modifies data
     *   - Not idempotent: Multiple calls create multiple resources
     *   - Returns 201 Created + location header
     *   - Example: Create new note
     *
     * PUT = Update (replace entire resource)
     *   - Not safe: Modifies data
     *   - Idempotent: Multiple calls result in same state
     *   - Should update entire resource
     *   - Example: Update note
     *
     * PATCH = Update (partial update)
     *   - Not safe: Modifies data
     *   - Not necessarily idempotent
     *   - Updates only specified fields
     *   - Example: Update just the title
     *   - We use PUT instead (simpler)
     *
     * DELETE = Delete (remove resource)
     *   - Not safe: Modifies data
     *   - Idempotent: Deleting same resource multiple times = same result
     *   - Returns 204 No Content
     *   - Example: Delete note
     */

    /**
     * RESPONSE ENTITY vs @ResponseBody:
     *
     * Option 1: ResponseEntity (what we use)
     * @GetMapping
     * public ResponseEntity<NoteDTO> getNote() {
     *     return ResponseEntity.ok(note);  // Control status code
     * }
     * Benefits:
     * - Control HTTP status code
     * - Add custom headers
     * - More flexible
     *
     * Option 2: Direct return (simpler but less control)
     * @GetMapping
     * public NoteDTO getNote() {
     *     return note;  // Always returns 200 OK
     * }
     * Benefits:
     * - Simpler code
     * - Less verbose
     * Drawbacks:
     * - Can't set status code
     * - Can't add headers
     *
     * We use ResponseEntity for flexibility and clarity!
     */
}
