package com.renoted.controller;

import com.renoted.dto.ApiResponse;
import com.renoted.dto.TagDTO;
import com.renoted.service.TagService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TAG CONTROLLER (UPDATED WITH ApiResponse)
 *
 * PURPOSE:
 * - Handles all tag-related HTTP requests
 * - Returns standardized ApiResponse
 *
 * IMPORTANT:
 * - No try-catch blocks (handled globally later)
 * - Clean separation of concerns
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * CREATE TAG
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TagDTO>> createTag(
            @Valid @RequestBody TagDTO tagDTO
    ) {

        TagDTO createdTag = tagService.createTag(tagDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Tag created successfully", createdTag)
        );
    }

    /**
     * GET ALL TAGS
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagDTO>>> getAllTags() {

        List<TagDTO> tags = tagService.getAllTags();

        return ResponseEntity.ok(
                ApiResponse.success("Tags fetched successfully", tags)
        );
    }

    /**
     * GET TAG BY ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagById(
            @PathVariable Long id
    ) {

        TagDTO tag = tagService.getTagById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Tag fetched successfully", tag)
        );
    }

    /**
     * DELETE TAG
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(
            @PathVariable Long id
    ) {

        tagService.deleteTag(id);

        return ResponseEntity.ok(
                ApiResponse.success("Tag deleted successfully", null)
        );
    }
}