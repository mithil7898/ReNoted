package com.renoted.service;

import com.renoted.dto.NoteDTO;
import com.renoted.entity.Note;
import com.renoted.entity.Tag;
import com.renoted.entity.User;
import com.renoted.repo.NoteRepo;
import com.renoted.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * UNIT TESTS FOR NoteService
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PURPOSE:
 * Test the business logic of NoteService in isolation.
 * Ensure that note creation, retrieval, updating, and deletion work correctly.
 *
 * KEY TESTING CONCEPTS:
 * 1. Unit Test - Test ONE class in isolation
 * 2. Mock - Fake objects that replace real dependencies
 * 3. Assert - Verify that expected results occurred
 * 4. Arrange-Act-Assert - Test structure pattern
 *
 * WHAT IS A UNIT TEST?
 * ─────────────────────
 * A unit test verifies that a single piece of code (usually one method)
 * works correctly in isolation. It tests the behavior, not the implementation.
 *
 * Example:
 * - Don't test: "Does createNote call noteRepo.save()?"
 * - DO test: "Does createNote return a NoteDTO with correct data?"
 *
 * WHY UNIT TESTS?
 * ───────────────
 * 1. CONFIDENCE
 *    - Know your code works
 *    - Catch bugs before production
 *    - Sleep better at night!
 *
 * 2. DOCUMENTATION
 *    - Tests show how to use the code
 *    - Examples of expected behavior
 *    - Clear contracts
 *
 * 3. REFACTORING SAFETY
 *    - Change code without fear
 *    - Tests ensure behavior unchanged
 *    - Easy to identify breaking changes
 *
 * 4. DESIGN IMPROVEMENT
 *    - Harder to test = poorly designed
 *    - Forces good architecture
 *    - Loose coupling, high cohesion
 *
 * WHY MOCKING?
 * ────────────
 * We use Mockito to create fake objects:
 * - Replace real database with mock
 * - Replace real security with mock
 * - Test NoteService in isolation
 * - Tests run fast (no database access)
 * - Tests are reliable (no network issues)
 *
 * TEST STRUCTURE:
 * ───────────────
 * 1. SETUP (Before each test)
 *    - Create test fixtures
 *    - Configure mocks
 *    - Prepare test data
 *
 * 2. ARRANGE (Within test method)
 *    - Set up specific test data
 *    - Configure mock behavior
 *    - Prepare inputs
 *
 * 3. ACT (Within test method)
 *    - Call the method being tested
 *    - Perform the action
 *    - Capture the result
 *
 * 4. ASSERT (Within test method)
 *    - Verify the result
 *    - Check side effects
 *    - Confirm expectations
 *
 * EXAMPLE STRUCTURE:
 *
 * @Test
 * void testSomething() {
 *     // ARRANGE - prepare data
 *     NoteDTO input = new NoteDTO("Title", "Content");
 *     User mockUser = new User("alice");
 *
 *     // ACT - call the method
 *     NoteDTO result = noteService.createNote(input);
 *
 *     // ASSERT - verify result
 *     assertNotNull(result);
 *     assertEquals("Title", result.getTitle());
 * }
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * @ExtendWith(MockitoExtension.class)
 *
 * PURPOSE: Enable Mockito in JUnit 5
 *
 * What is @ExtendWith?
 * - Plugs in a JUnit extension
 * - Mockito provides MockitoExtension
 * - Initializes @Mock and @InjectMocks before each test
 *
 * Why needed?
 * - Without it, @Mock and @InjectMocks don't work
 * - Mockito wouldn't initialize mocks
 * - Tests would fail with NullPointerException
 *
 * Alternative (older style):
 * @RunWith(MockitoJUnitRunner.class)  // JUnit 4 style
 *a
 * Modern approach (JUnit 5):
 * @ExtendWith(MockitoExtension.class)  // ← What we use
 *
 * HOW IT WORKS:
 * 1. JUnit detects @ExtendWith annotation
 * 2. Loads MockitoExtension
 * 3. Before each test: MockitoExtension initializes mocks
 * 4. Each @Mock field gets a mock object
 * 5. @InjectMocks gets dependencies injected
 * 6. Test method runs
 * 7. Mocks are reset for next test
 */
@ExtendWith(MockitoExtension.class)

/**
 * @DisplayName("NoteService Unit Tests")
 *
 * PURPOSE: Human-readable test class name
 *
 * What is @DisplayName?
 * - Provides readable name for test output
 * - Shows in IDE and test reports
 * - Makes tests easier to understand
 *
 * Without @DisplayName:
 * Tests → NoteServiceTest → testCreateNote
 *
 * With @DisplayName:
 * Tests → NoteService Unit Tests → [test name]
 *
 * Benefits:
 * - Clear test reports
 * - Better for non-technical people
 * - Markdown support: allows spaces and special chars
 */
@DisplayName("NoteService Unit Tests")
public class NoteServiceTest {

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * MOCKING THE DEPENDENCIES
     * ═══════════════════════════════════════════════════════════════════════
     *
     * @Mock - Creates a mock object
     *
     * What is a mock?
     * - A fake object that behaves like a real object
     * - Lets us control its behavior
     * - Records interactions with it
     * - Helps test in isolation
     *
     * Why use mocks?
     * - Don't want to hit real database in tests
     * - Don't want to depend on external services
     * - Want fast, reliable tests
     * - Want to isolate the code under test
     *
     * Process:
     * 1. @Mock annotation tells Mockito: "Create a mock"
     * 2. MockitoExtension creates the mock before test runs
     * 3. We configure behavior with when().thenReturn()
     * 4. During test, mock responds as we programmed
     * 5. After test, we can verify calls made to mock
     */

    /**
     * Mock NoteRepo
     *
     * This is a fake repository. Instead of touching the real database,
     * the mock lets us control what it returns.
     *
     * Example:
     * when(noteRepo.save(any(Note.class))).thenReturn(savedNote);
     * // Means: When save() is called, return savedNote
     *
     * Why mock it?
     * - Real repository would need database running
     * - Tests would be slow (database I/O)
     * - Tests would depend on database state
     * - Tests would fail if database down
     *
     * With mock:
     * - No database needed
     * - Tests run in milliseconds
     * - Tests always consistent
     * - Tests run offline
     */
    @Mock
    private NoteRepo noteRepo;

    /**
     * Mock TagService
     *
     * Service for managing tags.
     * We mock it because we're testing NoteService, not TagService.
     *
     * Focus on one thing:
     * This test should verify NoteService logic, not tag logic.
     * Tag logic is tested separately in TagServiceTest.
     */
    @Mock
    private TagService tagService;

    /**
     * Mock SecurityUtils
     *
     * Provides current authenticated user.
     * We mock it to simulate different users.
     *
     * Example:
     * when(securityUtils.getCurrentUser()).thenReturn(mockUser);
     * // Means: When getCurrentUser() called, return mockUser
     *
     * Why not call real SecurityUtils?
     * - Real SecurityUtils reads from SecurityContext
     * - SecurityContext needs Spring Security setup
     * - Too complicated for unit test
     * - Would blur the line of what we're testing
     */
    @Mock
    private SecurityUtils securityUtils;

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * INJECT MOCKS INTO SERVICE
     * ═══════════════════════════════════════════════════════════════════════
     *
     * @InjectMocks - Creates real object with mocked dependencies
     *
     * What does it do?
     * - Creates new NoteService instance
     * - Finds all @Mock fields
     * - Injects them into NoteService constructor/setters
     * - Result: Real NoteService with fake dependencies
     *
     * Example:
     * @InjectMocks
     * private NoteService noteService;
     *
     * Mockito does:
     * 1. new NoteService(noteRepo, tagService, securityUtils)
     * 2. All three dependencies are mocks
     * 3. NoteService is real (can call its methods)
     * 4. But its dependencies are fakes
     *
     * Result:
     * - Test NoteService logic
     * - Don't test repository, tags, or security
     * - Isolated unit test
     *
     * Without @InjectMocks:
     * private NoteService noteService;
     * // Would be null, need to manually create
     *
     * With @InjectMocks:
     * // Mockito automatically creates it
     * // Much cleaner!
     */
    @InjectMocks
    private NoteService noteService;

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * TEST FIXTURES - Reusable test data
     * ═══════════════════════════════════════════════════════════════════════
     *
     * We create these once (in @BeforeEach) and reuse in multiple tests.
     * This keeps tests DRY (Don't Repeat Yourself).
     *
     * What is a fixture?
     * - Pre-prepared test data
     * - Set up before each test
     * - Available to all tests in class
     * - Reset before each test
     *
     * Benefits:
     * - Consistent test data
     * - Less code duplication
     * - Easy to maintain
     * - Clear test intent
     */

    // A test user (Alice)
    private User testUser;

    // A test note
    private Note testNote;

    // A test note DTO
    private NoteDTO testNoteDTO;

    /**
     * @BeforeEach - Run before EACH test method
     *
     * Purpose: Set up test fixtures
     *
     * When does it run?
     * - Before every @Test method
     * - Creates fresh data for each test
     * - Ensures tests don't interfere with each other
     *
     * Example:
     * @Test void test1() { ... }
     * @Test void test2() { ... }
     *
     * Execution order:
     * 1. @BeforeEach setup() → creates fixtures
     * 2. @Test test1() → runs with fresh fixtures
     * 3. @BeforeEach setup() → resets fixtures
     * 4. @Test test2() → runs with fresh fixtures
     *
     * Why important?
     * - Tests are isolated
     * - One test can't affect another
     * - Data consistency
     * - Reliable tests
     */
    @BeforeEach
    void setUp() {
        /*
         * CREATE TEST USER
         *
         * This simulates a real user in the system.
         *
         * User details:
         * - ID: 1 (assigned by database)
         * - Username: "alice"
         * - Email: alice@example.com
         * - Enabled: true (account is active)
         *
         * We'll use this user for all tests.
         * Tests will verify that notes are correctly associated with this user.
         */
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
        testUser.setFullName("Alice Smith");
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);
        testUser.setPassword("hashedPassword123"); // Already hashed by BCrypt
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        /*
         * CREATE TEST NOTE DTO (Data Transfer Object)
         *
         * This is what the frontend sends when creating/updating a note.
         * DTOs are simple objects with just the data we need.
         *
         * Note about DTOs:
         * - No ID (database generates it)
         * - No timestamps (database sets them)
         * - Contains data from user input
         * - Validated by @NotBlank, @Size, etc.
         *
         * Fields:
         * - title: "My First Note" (required, validated)
         * - content: "This is test content" (optional)
         * - tagIds: null (no tags for this basic test)
         */
        testNoteDTO = new NoteDTO();
        testNoteDTO.setTitle("My First Note");
        testNoteDTO.setContent("This is test content for my note");
        testNoteDTO.setTagIds(null); // No tags in this basic test

        /*
         * CREATE TEST NOTE ENTITY (Database representation)
         *
         * This is what gets stored in the database.
         * Entities have:
         * - ID (auto-generated by database)
         * - User reference (shows ownership)
         * - Timestamps (auto-managed)
         *
         * Unlike DTO:
         * - Has ID: 1 (assigned by database)
         * - Has user: alice (ownership)
         * - Has timestamps: created_at, updated_at
         * - These are NOT sent to frontend directly
         *
         * This entity represents what the database returns
         * after saving testNoteDTO.
         */
        testNote = new Note();
        testNote.setId(1L); // Database would assign this
        testNote.setTitle("My First Note");
        testNote.setContent("This is test content for my note");
        testNote.setUser(testUser); // Alice owns this note
        testNote.setTags(new HashSet<>()); // Empty tags for now
        testNote.setCreatedAt(LocalDateTime.now());
        testNote.setUpdatedAt(LocalDateTime.now());

        /*
         * WHY CREATE ALL THREE?
         *
         * DTO (testNoteDTO):
         * - What frontend sends
         * - What we receive in Controller
         * - Data to be saved
         *
         * Entity (testNote):
         * - What gets saved to database
         * - Has ID and timestamps
         * - What repository returns
         *
         * User (testUser):
         * - Who owns the note
         * - Provided by SecurityUtils
         * - Sets user ownership
         *
         * In tests:
         * 1. Frontend sends testNoteDTO to API
         * 2. Controller calls noteService.createNote(testNoteDTO)
         * 3. Service calls securityUtils.getCurrentUser() → testUser
         * 4. Service converts DTO to Entity (testNote)
         * 5. Service calls noteRepo.save(testNote)
         * 6. Mock repository returns testNote with ID
         * 7. Service converts Entity back to DTO
         * 8. API returns DTO to frontend
         */
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * FIRST TEST: Create Note Successfully
     * ═══════════════════════════════════════════════════════════════════════
     *
     * TEST NAME: testCreateNoteSuccessfully
     *
     * WHAT ARE WE TESTING?
     * ────────────────────
     * The happy path: Creating a note with valid data works correctly.
     *
     * SCENARIO:
     * - Alice is logged in
     * - Alice provides note title and content
     * - System should create note and return it
     *
     * REQUIREMENTS WE'RE VERIFYING:
     * 1. getCurrentUser() should be called (to set owner)
     * 2. Note data should be correct (title, content)
     * 3. User should be set as owner
     * 4. Note should be saved to database
     * 5. Returned DTO should have ID and timestamps
     * 6. Response should match input data
     *
     * WHY THIS TEST MATTERS:
     * - Core functionality of NoteService
     * - If this breaks, users can't create notes
     * - Tests security (owner verification)
     * - Tests data integrity
     */
    @Test
    @DisplayName("Should create note successfully with owner")
    void testCreateNoteSuccessfully() {
        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 1: ARRANGE - Set up test data and mock behavior
         * ═══════════════════════════════════════════════════════════════════
         *
         * Purpose: Prepare everything the test needs
         *
         * What we do here:
         * 1. Configure mock to return test user
         * 2. Configure mock to return saved note
         * 3. Prepare input data
         * 4. Don't call actual methods yet!
         */

        /*
         * CONFIGURE MOCK: SecurityUtils.getCurrentUser()
         *
         * What we're saying:
         * "When noteService calls securityUtils.getCurrentUser(),
         *  return our testUser (alice)"
         *
         * Syntax: when(...).thenReturn(...)
         * - when(): Start mock configuration
         * - securityUtils.getCurrentUser(): The method being mocked
         * - thenReturn(testUser): What to return
         *
         * Real flow (in production):
         * - User sends request with JWT token
         * - JwtAuthenticationFilter validates token
         * - Sets SecurityContext
         * - securityUtils.getCurrentUser() reads from context
         * - Returns actual User from database
         *
         * Test flow (with mock):
         * - We bypass all that
         * - Mock directly returns testUser
         * - Much simpler, faster, isolated
         *
         * Why mock it?
         * - Don't need Spring Security setup in unit test
         * - Don't need JWT validation
         * - Don't need database to find user
         * - Just test: "If we have user Alice, does createNote work?"
         */
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        /*
         * CONFIGURE MOCK: NoteRepo.save()
         *
         * What we're saying:
         * "When noteService calls noteRepo.save(any Note),
         *  return our testNote with ID and timestamps"
         *
         * Syntax explanation:
         * - when(noteRepo.save(...)): "When save is called..."
         * - any(Note.class): "...with any Note object..."
         * - .thenReturn(testNote): "...return testNote"
         *
         * any(Note.class)?
         * - We don't care about the exact Note object passed
         * - Just want to know save() was called
         * - Return testNote as the saved result
         *
         * Real flow (production):
         * - noteRepo.save(note) goes to database
         * - Database generates ID (via IDENTITY)
         * - Database sets timestamps
         * - Returns note with ID
         *
         * Test flow (with mock):
         * - noteRepo.save(note) doesn't touch database
         * - Mock immediately returns testNote
         * - testNote already has ID and timestamps
         * - No actual SQL, no network, no delays
         *
         * Why this approach?
         * - Fast (no database I/O)
         * - Reliable (no database needed)
         * - Focused (only test NoteService logic)
         */
        when(noteRepo.save(any(Note.class))).thenReturn(testNote);

        /*
         * ACTUAL INPUT DATA
         *
         * This is what the frontend sends (simulated).
         * testNoteDTO was created in @BeforeEach.
         *
         * Contains:
         * - title: "My First Note"
         * - content: "This is test content for my note"
         * - tagIds: null
         *
         * Does NOT contain:
         * - id (will be generated)
         * - user (will be set from currentUser)
         * - timestamps (will be generated)
         *
         * This mirrors a real frontend request:
         * POST /api/notes
         * {
         *   "title": "My First Note",
         *   "content": "This is test content for my note"
         * }
         */
        // testNoteDTO already prepared in @BeforeEach

        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 2: ACT - Call the method being tested
         * ═══════════════════════════════════════════════════════════════════
         *
         * Purpose: Execute the code we're testing
         *
         * What we do here:
         * - Call ONE method
         * - That's it!
         * - Don't verify yet, don't assert yet
         * - Just execute
         *
         * Why separate Act from Assert?
         * - Clear what we're testing
         * - Easy to see what's under test
         * - Better code organization
         * - Matches AAA pattern (Arrange-Act-Assert)
         */

        /*
         * CALL THE METHOD
         *
         * noteService.createNote(testNoteDTO)
         *
         * What happens internally:
         * 1. Calls securityUtils.getCurrentUser()
         *    → Returns testUser (our mock)
         * 2. Converts testNoteDTO to Note entity
         * 3. Sets testUser as owner (note.setUser(testUser))
         * 4. Calls noteRepo.save(note)
         *    → Returns testNote with ID (our mock)
         * 5. Converts saved Note back to NoteDTO
         * 6. Returns NoteDTO to us
         *
         * Result stored in 'result':
         * - NoteDTO with:
         *   - id: 1
         *   - title: "My First Note"
         *   - content: "This is test content for my note"
         *   - createdAt: now
         *   - updatedAt: now
         *
         * NOTE: We don't verify mocks here!
         * That comes in the Assert phase.
         */
        NoteDTO result = noteService.createNote(testNoteDTO);

        /*
         * ═══════════════════════════════════════════════════════════════════
         * STEP 3: ASSERT - Verify the results
         * ═══════════════════════════════════════════════════════════════════
         *
         * Purpose: Verify that the method worked correctly
         *
         * What we check:
         * 1. Return value is not null
         * 2. Return value has correct data
         * 3. Mocks were called correctly
         * 4. No unexpected side effects
         *
         * Multiple levels of assertions:
         * - Data assertions: assertNotNull, assertEquals
         * - Mock verification: verify(mock).method()
         */

        /*
         * ASSERTION 1: Result is not null
         *
         * assertNotNull(result)
         *
         * Verifies: The method returned a value (not null)
         *
         * Why check this?
         * - If null, something went wrong
         * - Could indicate exception was silently caught
         * - First sanity check
         *
         * What it tests:
         * - Method completed successfully
         * - No exceptions thrown
         * - Returned an object
         *
         * If this fails:
         * ❌ AssertionError: expected non-null but was null
         * Message: createNote returned null (shouldn't happen)
         *
         * Example of null result:
         * - Service threw exception
         * - Return statement missing
         * - Mocks not configured
         */
        assertNotNull(result, "Created note should not be null");

        /*
         * ASSERTION 2: ID is set
         *
         * assertNotNull(result.getId())
         *
         * Verifies: The returned note has an ID
         *
         * Why important?
         * - Database assigns ID
         * - Without ID, note can't be found later
         * - Frontend needs ID to manage note
         *
         * What it tests:
         * - Entity was properly saved
         * - Database generated ID
         * - DTO was properly created from entity
         *
         * If this fails:
         * ❌ AssertionError: expected non-null but was null
         * Message: ID is null (not saved properly)
         *
         * Expected:
         * ✅ 1 (or any positive number)
         */
        assertNotNull(result.getId(), "Note ID should be generated and set");

        /*
         * ASSERTION 3: ID matches expected value
         *
         * assertEquals(1L, result.getId())
         *
         * Verifies: The ID is exactly what we expect
         *
         * assertEquals syntax:
         * assertEquals(expected, actual, "error message")
         *
         * Why important?
         * - Not just any ID, the RIGHT ID
         * - Ensures correct note was returned
         * - Prevents wrong data leakage
         *
         * What it tests:
         * - Correct entity was saved
         * - Correct entity was returned
         * - Proper mapping from entity to DTO
         *
         * If this fails:
         * ❌ AssertionError: expected <1> but was <2>
         * Message: Wrong ID returned (mapped wrong note)
         *
         * Expected: 1
         * Actual: 2
         */
        assertEquals(1L, result.getId(), "Note ID should match the saved note");

        /*
         * ASSERTION 4: Title is preserved
         *
         * assertEquals("My First Note", result.getTitle())
         *
         * Verifies: The title matches what was sent
         *
         * Why important?
         * - Title must not be lost or corrupted
         * - Conversion DTO→Entity→DTO must preserve data
         * - User sees correct data
         *
         * What it tests:
         * - Title correctly stored
         * - Title correctly retrieved
         * - No data loss in conversions
         *
         * If this fails:
         * ❌ AssertionError: expected <My First Note> but was <null>
         * Message: Title lost (conversion failed)
         *
         * Could indicate:
         * - convertToEntity() doesn't set title
         * - convertToDTO() doesn't get title
         * - Data lost in database
         */
        assertEquals("My First Note", result.getTitle(), "Title should match input");

        /*
         * ASSERTION 5: Content is preserved
         *
         * assertEquals("This is test content...", result.getContent())
         *
         * Same reasoning as title.
         * Verify content survives the round trip:
         * Frontend → DTO → Entity → Database → Entity → DTO → Frontend
         */
        assertEquals("This is test content for my note", result.getContent(),
                "Content should match input");

        /*
         * ASSERTION 6: Timestamps are set
         *
         * assertNotNull(result.getCreatedAt())
         * assertNotNull(result.getUpdatedAt())
         *
         * Verifies: Database timestamps were applied
         *
         * Why important?
         * - Timestamps auto-managed by Hibernate
         * - Required for audit trails
         * - Required for "created on" display
         *
         * What it tests:
         * - Entity timestamps were set
         * - DTO includes timestamps
         * - Proper entity lifecycle
         *
         * If this fails:
         * ❌ AssertionError: expected non-null but was null
         * Message: Timestamps not set (Hibernate failed)
         */
        assertNotNull(result.getCreatedAt(), "createdAt timestamp should be set");
        assertNotNull(result.getUpdatedAt(), "updatedAt timestamp should be set");

        /*
         * ASSERTION 7: Verify mock was called correctly
         *
         * verify(securityUtils).getCurrentUser();
         *
         * Verifies: The mock method was called
         *
         * What is verify?
         * - Check that a mock method was called
         * - Check it was called with right arguments
         * - Check it was called right number of times
         *
         * Syntax:
         * verify(mock).methodName();  // Called once (default)
         * verify(mock, times(2)).methodName();  // Called twice
         * verify(mock, never()).methodName();  // Never called
         * verify(mock).methodName(eq(value));  // Called with specific arg
         *
         * Why verify getCurrentUser?
         * - Must be called to get user
         * - Proves service calls security correctly
         * - Confirms owner is set
         * - Security validation
         *
         * If this fails:
         * ❌ Mockito: Wanted but not invoked
         * Message: getCurrentUser() was never called
         *
         * Could indicate:
         * - Service doesn't set user
         * - Service bypasses security
         * - No ownership assigned
         *
         * Security implication:
         * - Notes wouldn't have owners!
         * - Multi-tenant data leakage!
         * - Critical bug!
         */
        verify(securityUtils, times(1)).getCurrentUser();

        /*
         * ASSERTION 8: Verify save was called
         *
         * verify(noteRepo).save(any(Note.class));
         *
         * Verifies: The repository save method was called
         *
         * Why verify save?
         * - Proves note was actually persisted
         * - Proves database interaction occurred
         * - Ensures CRUD operation completed
         *
         * any(Note.class)?
         * - We don't care about exact Note object
         * - Just want to verify save was called
         * - Could also verify with: argThat(...)
         *
         * If this fails:
         * ❌ Mockito: Wanted but not invoked
         * Message: save() was never called
         *
         * Could indicate:
         * - Service doesn't persist note
         * - Early return before save
         * - Exception caught silently
         * - Note exists only in memory
         */
        verify(noteRepo, times(1)).save(any(Note.class));

        /*
         * ═══════════════════════════════════════════════════════════════════
         * TEST COMPLETE
         * ═══════════════════════════════════════════════════════════════════
         *
         * Summary of what we verified:
         * ✅ Method returned non-null NoteDTO
         * ✅ Returned DTO has correct ID
         * ✅ Returned DTO has correct title
         * ✅ Returned DTO has correct content
         * ✅ Timestamps were set
         * ✅ getCurrentUser() was called (owner set)
         * ✅ save() was called (persisted to DB)
         *
         * Conclusions:
         * ✓ createNote works correctly
         * ✓ Security implemented (owner set)
         * ✓ Data integrity maintained
         * ✓ Persistence works
         * ✓ DTO conversions work
         *
         * What we didn't test:
         * ✗ HTML sanitization (that's convertToEntity's job)
         * ✗ Tag handling (that's TagService's job)
         * ✗ Database constraints (that's Hibernate's job)
         * ✗ Validation (that's Controller/DTO's job)
         *
         * Why not test those?
         * - Different units, different tests
         * - Each class/method should have its own test
         * - Unit test = one thing in isolation
         * - Integration test = multiple parts together
         */
    }

    @Test
    @DisplayName("Should retrieve note when user is authorized")
    void testGetNoteByIdAuthorized() {

        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        when(noteRepo.findByIdAndUser(testNote.getId(), testUser)).thenReturn(Optional.of(testNote));

        NoteDTO result = noteService.getNoteById(testNote.getId());

        assertNotNull(result, "Retrieved note should not be null");
        assertEquals(testNote.getId(), result.getId(), "Note ID should match");
        assertEquals(testNote.getTitle(), result.getTitle(), "Title should match");

        verify(securityUtils, times(1)).getCurrentUser();
        verify(noteRepo, times(1)).findByIdAndUser(testNote.getId(), testUser);
    }

    @Test
    @DisplayName("Should throw exception when user is unauthorized to access note")
    void testGetNoteByIdUnauthorized() {

        // Step 0 - Create another user (Bob)
        User otherUser = new User();

        otherUser.setId(2L);
        otherUser.setUsername("bob");
        otherUser.setEmail("bob@example.com");
        otherUser.setFullName("Bob Builder");
        otherUser.setRole("ROLE_USER");
        otherUser.setEnabled(true);
        otherUser.setPassword("hashedPassword1234"); // Already hashed by BCrypt
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());

        Note otherUserNote = new Note();
        otherUserNote.setId(99L); // Database would assign this
        otherUserNote.setTitle("Other user's Note");
        otherUserNote.setContent("content for other note");
        otherUserNote.setUser(otherUser); // Alice owns this note
        otherUserNote.setTags(new HashSet<>()); // Empty tags for now
        otherUserNote.setCreatedAt(LocalDateTime.now());
        otherUserNote.setUpdatedAt(LocalDateTime.now());

        // Step 1 - Mock SecurityUtils to return Alice
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        // Step 2 - Mock NoteRepo to return Bob's note when searching for that ID
        when(noteRepo.findByIdAndUser(otherUserNote.getId(), testUser)).thenReturn(Optional.empty());

        // Step 3 - Call getNoteById with Bob's note ID and expect an exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            noteService.getNoteById(otherUserNote.getId());
        });

        // Step 4 - Verify that the exception message is correct
        assertEquals("Note not found or access denied", exception.getMessage(),
                "Exception message should indicate access denied");
    }

    /*
     * ═══════════════════════════════════════════════════════════════════════
     * MORE TESTS WOULD GO HERE
     * ═══════════════════════════════════════════════════════════════════════
     *
     * Examples of other tests we should write:
     *
     * @Test
     * void testCreateNoteWithTags() { ... }
     * - Test creating note with tags
     * - Verify tags are associated
     * - Verify tagService called correctly
     *
     * @Test
     * void testGetAllNotes() { ... }
     * - Test fetching all notes for user
     * - Verify only user's notes returned
     * - Verify filtering by user
     *
     * @Test
     * void testGetNoteByIdAuthorized() { ... }
     * - Test accessing own note
     * - Verify note is returned
     *
     * @Test
     * void testGetNoteByIdUnauthorized() { ... }
     * - Test accessing another user's note
     * - Verify exception is thrown
     * - Security test!
     *
     * @Test
     * void testUpdateNote() { ... }
     * - Test updating note data
     * - Verify ownership still intact
     * - Verify user can't change owner
     *
     * @Test
     * void testDeleteNote() { ... }
     * - Test deleting note
     * - Verify repository delete called
     *
     * @Test
     * void testSearchNotes() { ... }
     * - Test searching within user's notes
     * - Verify search results scoped to user
     *
     * etc.
     *
     * ═══════════════════════════════════════════════════════════════════════
     */

}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * KEY TESTING CONCEPTS SUMMARY
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 1. UNIT TEST
 *    - Tests ONE class/method
 *    - In isolation from other classes
 *    - Uses mocks for dependencies
 *    - Fast and focused
 *
 * 2. MOCK / STUB
 *    - Fake object replacing real dependency
 *    - We control its behavior
 *    - Records interactions
 *    - Enables isolated testing
 *
 * 3. WHEN / THEN
 *    - when(mock.method()).thenReturn(value)
 *    - Sets up mock behavior
 *    - Configures what happens
 *    - Before method is called
 *
 * 4. VERIFY
 *    - verify(mock).method()
 *    - Checks if method was called
 *    - Verifies interactions
 *    - After method execution
 *
 * 5. ASSERT
 *    - assertEquals(expected, actual)
 *    - Verifies results
 *    - Checks state after execution
 *    - Validates behavior
 *
 * 6. AAA PATTERN
 *    - Arrange: Set up test data
 *    - Act: Call method under test
 *    - Assert: Verify results
 *    - Clear structure
 *
 * 7. FIXTURES
 *    - Reusable test data
 *    - Created in @BeforeEach
 *    - Available to all tests
 *    - Keeps tests DRY
 *
 * 8. ISOLATION
 *    - Each test independent
 *    - Don't affect each other
     - Reset between tests
     - Clean slate each time
     *
     * ═══════════════════════════════════════════════════════════════════════
     */

