package com.renoted.controller;

import com.renoted.dto.ApiResponse;
import com.renoted.dto.NoteDTO;
import com.renoted.service.NoteService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NOTE CONTROLLER (UPDATED WITH ApiResponse)
 *
 * PURPOSE:
 * - Handles all note-related HTTP requests
 * - Wraps responses in ApiResponse for consistency
 *
 * IMPORTANT:
 * - Service layer remains unchanged
 * - Only Controller handles ApiResponse wrapping
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * CREATE NOTE
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NoteDTO>> createNote(
            @Valid @RequestBody NoteDTO noteDTO
    ) {
        NoteDTO createdNote = noteService.createNote(noteDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Note created successfully", createdNote)
        );
    }

    /**
     * GET ALL NOTES
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteDTO>>> getAllNotes() {

        List<NoteDTO> notes = noteService.getAllNotes();

        return ResponseEntity.ok(
                ApiResponse.success("Notes fetched successfully", notes)
        );
    }

    /**
     * GET NOTE BY ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteDTO>> getNoteById(
            @PathVariable Long id
    ) {

        NoteDTO note = noteService.getNoteById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Note fetched successfully", note)
        );
    }

    /**
     * UPDATE NOTE
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteDTO>> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteDTO noteDTO
    ) {

        NoteDTO updatedNote = noteService.updateNote(id, noteDTO);

        return ResponseEntity.ok(
                ApiResponse.success("Note updated successfully", updatedNote)
        );
    }

    /**
     * DELETE NOTE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable Long id
    ) {

        noteService.deleteNote(id);

        return ResponseEntity.ok(
                ApiResponse.success("Note deleted successfully", null)
        );
    }

    /**
     * SEARCH NOTES
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<NoteDTO>>> searchNotes(
            @RequestParam(required = false) String query
    ) {

        List<NoteDTO> notes = noteService.searchNotes(query);

        return ResponseEntity.ok(
                ApiResponse.success("Search completed successfully", notes)
        );
    }

    /**
     * FILTER NOTES BY TAG
     */
    @GetMapping("/filter/tag/{tagId}")
    public ResponseEntity<ApiResponse<List<NoteDTO>>> filterNotesByTag(
            @PathVariable Long tagId
    ) {

        List<NoteDTO> notes = noteService.filterByTag(tagId);

        return ResponseEntity.ok(
                ApiResponse.success("Filtered notes fetched successfully", notes)
        );
    }
}