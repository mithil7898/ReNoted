# NoteController Test Setup - Complete Guide

## Problem Summary

The initial NoteControllerTest was getting **403 FORBIDDEN** status when trying to test the POST /api/notes endpoint.

### Root Cause
The main SecurityConfig requires JWT authentication for all `/api/**` endpoints. The test was blocked by Spring Security before the request even reached the controller.

## Solution

### Step 1: Added spring-security-test Dependency
**File: `pom.xml`**

Added the Spring Security Test library to support security testing:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Step 2: Created TestSecurityConfig
**File: `src/test/java/com/renoted/config/TestSecurityConfig.java`**

Created a test-specific security configuration that:
- Disables security for all endpoints during testing
- Allows tests to focus on controller logic, not authentication
- Only applies during test execution

```java
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}
```

### Step 3: Updated NoteControllerTest
**File: `src/test/java/com/renoted/controller/NoteControllerTest.java`**

Key changes:
1. Added `@Import(TestSecurityConfig.class)` annotation
2. Changed from pure unit test to @WebMvcTest approach
3. Added verbose documentation explaining each assertion
4. Used `any(NoteDTO.class)` for mock matching (not exact object matching)

```java
@WebMvcTest(controllers = NoteController.class)
@Import(TestSecurityConfig.class)
public class NoteControllerTest {
    // Test implementation
}
```

## Test Result

✅ **PASSED**
- HTTP Status: 201 CREATED
- Response includes ApiResponse with success=true
- Response includes created note data with ID, title, content
- All assertions pass

## Key Testing Concepts Explained

### @WebMvcTest
- Loads only the web layer (controller, filters)
- Faster than full application context
- Isolates controller testing

### @Import(TestSecurityConfig.class)
- Overrides main SecurityConfig for testing
- Allows unauthenticated access during tests
- Only applied in test context

### MockMvc
- Simulates HTTP requests without running a server
- Allows testing request/response cycle
- Supports POST, GET, PUT, DELETE, etc.

### any(NoteDTO.class)
- Matches ANY NoteDTO object
- Important because JSON deserialization creates new object instances
- Can't use exact object matching when JSON is involved

### jsonPath()
- XPath-like syntax for JSON validation
- `$.success` = check success field
- `$.data.title` = check nested field
- `.value(x)` = verify equals x

## Separation of Concerns

This test setup separates different types of testing:

| Test Type | Focus | What's Tested | What's Mocked |
|-----------|-------|---------------|---------------|
| **Controller Unit Test** | HTTP handling | Response format, status codes, ApiResponse wrapper | Service layer |
| **Service Unit Test** | Business logic | Service methods, data validation | Repository, Security |
| **Security Test** | Authentication | Auth filters, JWT validation | (separate test type) |
| **Integration Test** | Full flow | End-to-end with real database | Nothing |

## Running the Test

```bash
# Run single test
mvn test -Dtest=NoteControllerTest

# Run all tests
mvn test

# Run with verbose output
mvn test -X
```

## Future Tests to Write

Once you understand this pattern, create similar tests for:
1. `testGetAllNotes()` - GET /api/notes
2. `testGetNoteById()` - GET /api/notes/{id}
3. `testUpdateNote()` - PUT /api/notes/{id}
4. `testDeleteNote()` - DELETE /api/notes/{id}
5. `testCreateNoteValidationError()` - Test validation failures

## Common Issues & Solutions

### Issue: Getting 403 FORBIDDEN
**Solution:** Make sure `@Import(TestSecurityConfig.class)` is added to test class

### Issue: Mock not matching
**Solution:** Use `any(NoteDTO.class)` instead of exact object matching

### Issue: NullPointerException on mockMvc
**Solution:** Verify `@Autowired` is present (Spring injects at test time)

### Issue: Response body is null
**Solution:** Check if mock is returning correct data

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Spring Test Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/reference/testing/mvc-test-framework.html)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

