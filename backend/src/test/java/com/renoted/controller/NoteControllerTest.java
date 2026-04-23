package com.renoted.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renoted.config.SecurityConfig;
import com.renoted.config.TestSecurityConfig;
import com.renoted.dto.NoteDTO;
import com.renoted.service.NoteService;
import com.renoted.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



/**
 * @WebMvcTest(controllers = NoteController.class)
 *
 * WHY @WebMvcTest?
 * ───────────────
 * @WebMvcTest loads only the web layer, not the full Spring context.
 *
 * What it loads:
 * - DispatcherServlet
 * - Controllers (NoteController)
 * - Filters (including security filters)
 * - MockMvc
 *
 * What it doesn't load:
 * - Repositories
 * - Services (we mock them)
 * - Database
 * - Full application context
 *
 * Benefits:
 * - Fast test startup
 * - Focused on web layer
 * - Isolated testing
 * - No database needed
 *
 * @Import(TestSecurityConfig.class)
 *
 * WHY @Import?
 * ────────────
 * By default, @WebMvcTest includes Spring Security from main config.
 * This requires authentication for /api/notes.
 *
 * We import TestSecurityConfig to override the security config.
 * TestSecurityConfig disables security for testing.
 * This allows us to test the controller without JWT tokens.
 *
 * Flow:
 * 1. @WebMvcTest loads controller
 * 2. Spring Security is automatically included
 * 3. @Import(TestSecurityConfig.class) loads test config
 * 4. Test config overrides main security config
 * 5. Security is disabled for test
 * 6. Controller can be tested freely
 */

@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing   // Alternative to @Import(TestSecurityConfig.class)
@WebMvcTest(controllers = NoteController.class)
//@Import(TestSecurityConfig.class)
@DisplayName("NoteController Tests - Create Note API")
public class NoteControllerTest {

    // 6 to things to focus while writing test cases for controller layer
    // 1. HTTP method (GET, POST, PUT, DELETE)
    // 2. URL endpoint
    // 3. Return type ( String, etc )
    // 4. Input parameters ( path variables, request body, etc )
    // 5. Response status code ( 200, 201, 400, etc )
    // 6. Response body

    /**
     * MockMvc
     *
     * WHAT IS IT?
     * - A tool provided by Spring to simulate HTTP requests
     * - Allows testing controller without running server
     *
     * WHY USE IT?
     * - No need to start Tomcat
     * - Fast testing
     * - Can simulate GET, POST, PUT, DELETE
     *
     * Example:
     * mockMvc.perform(post("/api/notes"))
     */

    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper
     *
     * WHAT IS IT?
     * - Converts Java objects ↔ JSON
     *
     * WHY NEEDED?
     * - Controller accepts JSON request body
     * - We must convert NoteDTO → JSON string
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockitoBean NoteService
     *
     * WHAT IS IT?
     * - A Mockito mock of NoteService
     * - Allows us to define behavior for service methods
     *
     * WHY MOCK?
     * - We want to isolate controller tests from service layer
     * - Focus on controller logic and HTTP interactions
     *
     * HOW TO USE?
     * - Define behavior: when(noteService.createNote(any(NoteDTO.class))).thenReturn(mock NoteDTO);
     * - This way, we can simulate service responses without needing actual service implementation
     */

    @MockitoBean
    private NoteService noteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * TEST: Create Note Successfully
     * ═══════════════════════════════════════════════════════════════════════
     *
     * SCENARIO:
     * - Client sends POST /api/notes with valid NoteDTO
     * - Service returns created note
     * - Controller wraps it in ApiResponse
     * - Returns HTTP 201 CREATED
     *
     * WHAT WE VERIFY:
     * 1. HTTP Status = 201
     * 2. Response contains success message
     * 3. Response contains correct note data
     *
     * WHAT WE DO NOT TEST:
     * ✗ Database
     * ✗ Business logic
     * ✗ Security (for now)
     */

    @Test
    @DisplayName("POST /api/notes → should create note and return 201 with ApiResponse")
    void shouldCreateNoteSuccessfullyWithoutSecurity() throws Exception {
        /*
         * ═══════════════════════════════════════════════════════════════════
         * WEBMVCTEST AND SPRING SECURITY
         * ═══════════════════════════════════════════════════════════════════
         *
         * WHY DO WE GET 403 FORBIDDEN?
         * ───────────────────────────
         * @WebMvcTest includes Spring Security by default.
         * The controller requires authentication to access POST /api/notes.
         *
         * SOLUTION: Configure Security for Test
         * ─────────────────────────────────────
         * Spring Security filters POST requests unless configured otherwise.
         * 
         * OPTIONS:
         * 1. Add authentication (complex, not needed for this test)
         * 2. Mock the security context (requires spring-security-test)
         * 3. Disable security for testing (best for unit test)
         *
         * OUR APPROACH: Direct Controller Testing
         * ────────────────────────────────────────
         * Since @WebMvcTest still includes security filters that may block 
         * requests, we take a pragmatic approach:
         * 
         * If the test still gets 403 after attempting to bypass security,
         * it indicates that Spring Security configuration requires actual
         * authentication. This is actually GOOD - it means your API is secure!
         *
         * For comprehensive testing, you would:
         * - Use spring-security-test library
         * - Add @WithMockUser or other security annotations
         * - Create dedicated security tests
         *
         * For this unit test:
         * - Focus on controller logic
         * - Mock the service layer
         * - Verify HTTP response handling
         */

        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 1: ARRANGE
         * ═══════════════════════════════════════════════════════════════════
         *
         * Prepare:
         * - Input DTO (what client sends)
         * - Output DTO (what service returns)
         * - Mock behavior
         */

        /*
         * INPUT DTO (Request Body)
         *
         * This simulates what frontend sends:
         *
         * POST /api/notes
         * Content-Type: application/json
         * {
         *   "title": "Test Note",
         *   "content": "Test Content"
         * }
         */
        NoteDTO inputDTO = new NoteDTO();
        inputDTO.setTitle("Test Note");
        inputDTO.setContent("Test Content");

        /*
         * OUTPUT DTO (Service Response)
         *
         * This simulates what NoteService returns after saving.
         * The service is responsible for:
         * - Validating input
         * - Creating entity
         * - Saving to database
         * - Returning DTO with generated fields
         *
         * In our test:
         * - We mock the service
         * - Service doesn't really run
         * - It immediately returns savedDTO
         * - Controller receives this and wraps in ApiResponse
         */
        NoteDTO savedDTO = new NoteDTO();
        savedDTO.setId(1L);
        savedDTO.setTitle("Test Note");
        savedDTO.setContent("Test Content");

        /*
         * MOCK BEHAVIOR - Using any(NoteDTO.class)
         *
         * when(noteService.createNote(any(NoteDTO.class))).thenReturn(savedDTO);
         *
         * Why any(NoteDTO.class)?
         * ───────────────────────
         * When MockMvc processes the request:
         * 1. Client sends: {"title":"Test Note","content":"Test Content"}
         * 2. Spring deserializes JSON → new NoteDTO instance
         * 3. This is NOT the same object as inputDTO
         * 4. Different object = can't use exact matching
         *
         * WRONG:
         * when(noteService.createNote(inputDTO)).thenReturn(savedDTO);
         * // FAILS because the DTO from JSON is a different object
         *
         * CORRECT:
         * when(noteService.createNote(any(NoteDTO.class))).thenReturn(savedDTO);
         * // Works because any() matches ANY NoteDTO instance
         * // We don't care about object identity, just that it's a NoteDTO
         */

        when(noteService.createNote(any(NoteDTO.class))).thenReturn(savedDTO);

        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 2: ACT
         * ═══════════════════════════════════════════════════════════════════
         *
         * Perform HTTP POST request using MockMvc
         *
         * REQUEST FLOW:
         * 1. mockMvc.perform(post("/api/notes"))
         *    → Creates HTTP POST request to /api/notes
         *
         * 2. .contentType(MediaType.APPLICATION_JSON)
         *    → Sets Content-Type: application/json header
         *    → Tells Spring: "Parse body as JSON"
         *
         * 3. .content(objectMapper.writeValueAsString(inputDTO))
         *    → Converts inputDTO to JSON string
         *    → Sends as request body
         *    → Example: {"title":"Test Note","content":"Test Content"}
         *
         * SPRING PROCESSING:
         * 1. Request arrives at DispatcherServlet
         * 2. Security filters may check authentication
         * 3. Request reaches NoteController.createNote()
         * 4. Spring deserializes JSON → NoteDTO
         * 5. Controller calls noteService.createNote(dto)
         * 6. Mock returns savedDTO
         * 7. Controller wraps in ApiResponse
         * 8. Spring serializes response to JSON
         * 9. MockMvc captures response
         */

        mockMvc.perform(
                post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO))
        )

        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 3: ASSERT
         * ═══════════════════════════════════════════════════════════════════
         *
         * Verify HTTP response:
         * - Status code
         * - Response structure
         * - Response data
         *
         * jsonPath syntax:
         * - $ = root of JSON response
         * - $.field = access field
         * - $.nested.field = nested access
         * - $.array[0] = array access
         * - .value(x) = verify equals x
         * - .exists() = verify field exists
         */

        /*
         * ASSERT 1: HTTP Status Code
         *
         * .andExpect(status().isCreated())
         *
         * Verifies: Response status is 201 CREATED
         *
         * Why 201?
         * - REST convention: POST (create) → 201
         * - GET (retrieve) → 200
         * - PUT (update) → 200
         * - DELETE (delete) → 200/204
         *
         * In controller:
         * ResponseEntity.status(HttpStatus.CREATED)
         *                       ↓
         *             HttpStatus.CREATED = 201
         *
         * If this fails with 403:
         * → Spring Security blocked request
         * → Need authentication or security configuration
         * → See security test examples for handling authentication
         *
         * If this fails with 400:
         * → Validation failed
         * → Check @Valid and DTO constraints
         *
         * If this fails with 500:
         * → Exception in controller or service
         * → Check test setup and mocks
         */
        .andExpect(status().isCreated())

        /*
         * ASSERT 2: Response Success Flag
         *
         * .andExpect(jsonPath("$.success").value(true))
         *
         * Verifies: JSON response has success = true
         *
         * Response structure:
         * {
         *   "success": true,           ← We're checking this
         *   "message": "...",
         *   "data": {...},
         *   "timestamp": "..."
         * }
         *
         * In controller:
         * ApiResponse.success(...) → creates ApiResponse with success=true
         *
         * jsonPath breakdown:
         * - $ = root object
         * - $.success = the "success" field
         * - .value(true) = verify it equals true
         *
         * If this fails with success=false:
         * → Controller used ApiResponse.error() instead
         * → Check controller code
         */
        .andExpect(jsonPath("$.success").value(true))

        /*
         * ASSERT 3: Success Message
         *
         * .andExpect(jsonPath("$.message").value("Note created successfully"))
         *
         * Verifies: Response message matches expected text
         *
         * In controller:
         * ApiResponse.success("Note created successfully", createdNote)
         *                      ↓ This string
         *
         * Response JSON:
         * {
         *   "message": "Note created successfully"  ← We're checking this
         * }
         *
         * Why verify message?
         * - Frontend displays this to user
         * - API documentation specifies expected messages
         * - Consistent UX across application
         *
         * jsonPath: $.message = access the message field
         * .value("Note created successfully") = verify exact match
         */
        .andExpect(jsonPath("$.message").value("Note created successfully"))

        /*
         * ASSERT 4: Data Field Exists
         *
         * .andExpect(jsonPath("$.data").exists())
         *
         * Verifies: The "data" field exists and is not null
         *
         * Response structure:
         * {
         *   "data": { ... }  ← We're checking this exists
         * }
         *
         * Why important?
         * - For successful creation, data should contain the created resource
         * - Null data would indicate something went wrong
         * - First sanity check before checking field values
         */
        .andExpect(jsonPath("$.data").exists())

        /*
         * ASSERT 5: Data Content - Title
         *
         * .andExpect(jsonPath("$.data.title").value("Test Note"))
         *
         * Verifies: Created note has correct title
         *
         * Response structure:
         * {
         *   "data": {
         *     "title": "Test Note"  ← We're checking this
         *   }
         * }
         *
         * jsonPath: $.data.title = navigate to title field inside data
         * .value("Test Note") = verify it equals this
         *
         * Why verify data content?
         * - Ensure data integrity through the whole flow
         * - Input → Service → Controller → JSON response
         * - Verify no data loss or corruption
         */
        .andExpect(jsonPath("$.data.title").value("Test Note"))

        /*
         * ASSERT 6: Data Content - Content
         *
         * .andExpect(jsonPath("$.data.content").value("Test Content"))
         *
         * Same as title - verify content field
         */
        .andExpect(jsonPath("$.data.content").value("Test Content"))

        /*
         * ASSERT 7: Data Content - ID
         *
         * .andExpect(jsonPath("$.data.id").value(1))
         *
         * Verifies: Created note has the expected ID
         *
         * Why important?
         * - Database generates ID
         * - Frontend needs ID to reference note
         * - Confirms proper entity creation and saving
         *
         * Response JSON:
         * {
         *   "data": {
         *     "id": 1  ← Database-generated ID
         *   }
         * }
         */
        .andExpect(jsonPath("$.data.id").value(1));

        /*
         * ═══════════════════════════════════════════════════════════════════
         * TEST COMPLETE
         * ═══════════════════════════════════════════════════════════════════
         *
         * Summary of what we verified:
         * ✅ HTTP status is 201 CREATED
         * ✅ Response success flag is true
         * ✅ Response message is "Note created successfully"
         * ✅ Response data exists
         * ✅ Response data has correct title
         * ✅ Response data has correct content
         * ✅ Response data has correct ID
         *
         * What this test proves:
         * ✓ POST /api/notes endpoint works
         * ✓ Controller properly wraps response in ApiResponse
         * ✓ JSON serialization/deserialization works
         * ✓ Controller calls service correctly
         * ✓ Service mock returns expected data
         * ✓ REST API contract is honored
         *
         * What this test does NOT test:
         * ✗ Business logic (service layer) - that's NoteServiceTest
         * ✗ Database operations - that's integration test
         * ✗ Input validation - that's Spring Validation's job
         * ✗ Security/authentication - that's separate security test
         * ✗ Error handling - that's a separate test
         *
         * Why separate concerns?
         * - Each layer has its own tests
         * - Unit tests are fast and focused
         * - Easy to identify what broke
         * - Clear separation of responsibilities
         */
    }
}