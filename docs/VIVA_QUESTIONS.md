# AlumConnect — Viva Questions & Answers

> Comprehensive question bank covering architecture, implementation, security, testing, and design decisions for the AlumConnect project viva.

---

## 1. Project Overview & Objectives

### Q1: What is AlumConnect?

**A:** AlumConnect is a Java console-based Alumni Networking & Mentorship Platform that connects university students with alumni for structured career guidance and mentorship. It features role-based authentication, profile management, alumni discovery (browse/search/filter), and a complete mentorship request lifecycle (send → accept/reject → connection).

### Q2: What problem does AlumConnect solve?

**A:** Students lack structured channels to seek career guidance from experienced alumni, while alumni lack organized platforms to manage mentorship bandwidth. AlumConnect solves this by providing a formal system with role-based access, verified profiles, structured request workflows, and clear mentorship capacity management.

### Q3: What are the key features of the application?

**A:** 
- Student and alumni registration with role assignment
- Secure authentication with SHA-256 salted password hashing
- Role-based dashboards (Student Dashboard, Alumni Dashboard)
- Student and alumni profile management (create, view, update)
- Alumni directory browsing, keyword search, and multi-criteria filtering
- Mentorship request sending with topics and cover notes
- Alumni request management (view, accept, reject)
- Student request status tracking
- Mentorship connection management
- Role-based access control (RBAC) throughout

---

## 2. Architecture & Design

### Q4: What architecture does AlumConnect follow?

**A:** AlumConnect follows a **Layered Architecture** with four distinct layers:
1. **Presentation Layer** (`com.alumconnect.ui`) — Console UI classes handling user interaction
2. **Service Layer** (`com.alumconnect.service`) — Business logic and validation
3. **Repository Layer** (`com.alumconnect.repository`) — Data access using JDBC
4. **Infrastructure Layer** (`com.alumconnect.util`) — Database connections, security utilities, session management

Each layer only communicates with its adjacent layer — the UI never directly accesses the database.

### Q5: What design patterns are used in the project?

**A:**
- **Singleton Pattern:** `UserSession` (session management) and `ConnectionPool` (database connections) ensure a single shared instance across the application.
- **Repository Pattern:** Each entity has an interface (`UserRepository`, `StudentProfileRepository`, etc.) with JDBC implementations, enabling testability through in-memory fakes.
- **DTO Pattern:** Data Transfer Objects like `LoginResult`, `RegistrationResult`, and `MentorshipRequestResult` carry operation outcomes between layers.
- **Service Layer Pattern:** All business logic is centralized in service classes, keeping UI and data access clean.
- **Builder Pattern:** Used in model classes and filter criteria for readable object construction.

### Q6: Why did you choose a layered architecture over MVC?

**A:** A layered architecture provides clearer separation of concerns for a console application. MVC is typically used for web applications with a view-controller cycle. Our layers (UI → Service → Repository → DB) map naturally to the data flow in a non-web application while maintaining the same testability and maintainability benefits.

### Q7: How does data flow through the application?

**A:** 
1. **User input** → Presentation Layer (e.g., `AuthUI`) captures console input
2. **Service call** → UI passes data to Service Layer (e.g., `UserService.login()`)
3. **Database operation** → Service delegates to Repository Layer (e.g., `JdbcUserRepository.findByEmail()`)
4. **JDBC execution** → Repository uses `ConnectionPool` to get a connection, executes `PreparedStatement`
5. **Result propagation** → Results flow back up: Repository → Service (wraps in DTO) → UI (displays to user)

---

## 3. Database & JDBC

### Q8: What database does AlumConnect use?

**A:** MySQL 8.0 with the database name `alumconnect_db`. The application uses pure JDBC (no ORM like Hibernate) for all database operations, with parameterized `PreparedStatement` queries to prevent SQL injection.

### Q9: What are the main database tables?

**A:**
- `users` — Stores user credentials, roles, and basic info (id, name, email, password_hash, salt, role, department)
- `student_profiles` — Student-specific data linked to users (enrollment_number, semester, graduation_year, skills)
- `alumni_profiles` — Alumni-specific data linked to users (company, role, industry, experience, expertise, mentorship_status, max_mentees)
- `mentorship_requests` — Request records with foreign keys to student and alumni users (topic, cover_note, status, timestamps)

### Q10: How does the connection pool work?

**A:** `ConnectionPool` is a thread-safe singleton that maintains a fixed-size pool of reusable database connections. It uses `acquire()` to check out a connection and `release()` to return it. `PooledConnectionWrapper` wraps real connections to intercept `close()` calls and redirect them to `release()`, enabling transparent pooling with try-with-resources blocks. The pool size is configured in `database.properties`.

### Q11: Why use JDBC instead of Hibernate or JPA?

**A:** 
- **Learning purpose:** Pure JDBC demonstrates understanding of SQL, connection management, and the repository pattern without framework abstraction.
- **Lightweight:** No additional dependencies or configuration overhead.
- **Control:** Full control over SQL queries, connection lifecycle, and error handling.
- **Project scope:** For a college project, JDBC provides the right balance of complexity and educational value.

---

## 4. Security

### Q12: How are passwords stored?

**A:** Passwords are **never stored in plaintext**. We use SHA-256 hashing with a unique random salt per user:
1. A random salt is generated during registration using `SecureRandom`.
2. The password is concatenated with the salt and hashed using SHA-256.
3. Both the hash and salt are stored in the `users` table.
4. During login, the same salt is retrieved and the input password is hashed identically for comparison.

### Q13: What is role-based access control (RBAC) and how is it implemented?

**A:** RBAC restricts system access based on user roles (STUDENT or ALUMNI). Implementation:
- `UserSession.requireRole(Role)` — Throws `SecurityException` if the current user doesn't have the required role.
- `UserSession.requireStudent()` / `requireAlumni()` — Convenience methods for role checks.
- `UserSession.requireUserId(int)` — Ensures a user can only access their own resources.
- Each dashboard and service method enforces role checks before executing operations.
- Students cannot access alumni dashboard features and vice versa.

### Q14: How do you prevent SQL injection?

**A:** All database queries use `PreparedStatement` with parameterized placeholders (`?`). User input is never concatenated directly into SQL strings. For example:
```java
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
ps.setString(1, email);
```

### Q15: How is session management handled?

**A:** `UserSession` is a thread-safe singleton that tracks the currently authenticated user:
- `login(User)` — Sets the current user after successful authentication.
- `logout()` — Clears the current user (sets to null).
- `isLoggedIn()` — Checks if a session is active.
- `getCurrentUser()` — Returns `Optional<User>` for safe access.
- `requireRole()` — Enforces authorization before operations.
- The session is volatile for thread safety.

---

## 5. Testing

### Q16: What testing framework is used?

**A:** JUnit 5 (JUnit Jupiter) is our testing framework. Tests are organized by layer and feature, with both unit tests and integration tests.

### Q17: How many tests does the project have?

**A:** **540 tests** across **33 test classes**, with:
- 515 tests that pass without a database
- 25 integration tests that are gracefully skipped when MySQL is unavailable
- 0 failures, 0 errors

### Q18: How do you test without a database?

**A:** We use **in-memory fake repositories** as inner classes within test files. These fakes implement the repository interfaces but store data in `HashMap` or `ArrayList` instead of a database. This allows testing all business logic, UI workflows, and service methods without MySQL running.

### Q19: How do you test console UI classes?

**A:** We use **stream-based testing**:
- Input is simulated using `ByteArrayInputStream` containing pre-defined user inputs separated by newlines.
- Output is captured using `ByteArrayOutputStream` wrapped in a `PrintStream`.
- Each UI class accepts `InputStream` and `PrintStream` in its constructor, enabling injection of test streams.
- Tests verify that the captured output contains expected menu texts, messages, and data.

### Q20: What types of tests exist in the project?

**A:**
- **Model tests:** Verify domain object creation, getters, setters, role checks, and display labels.
- **Service tests:** Verify business logic, validation, error handling, and DTO results using fake repositories.
- **Repository tests:** Unit tests with fakes + integration tests with real MySQL.
- **UI tests:** Verify console output, menu navigation, input handling, and error display using stream injection.
- **Security tests:** Verify RBAC enforcement, session management, and unauthorized access prevention.
- **Workflow tests:** End-to-end tests covering complete user workflows (register → login → profile → request → accept).
- **Regression tests:** Verify bug fixes and edge cases remain fixed.

---

## 6. Models & Data

### Q21: What is the user model hierarchy?

**A:** 
- `User` — Base class with id, name, email, passwordHash, salt, role, department
- `Student extends User` — Adds enrollmentNumber, currentSemester, graduationYear, skills
- `Alumni extends User` — Adds company, currentRole, industry, yearsOfExperience, expertiseAreas, linkedInUrl, mentorshipStatus, maxMentees

The `Role` enum has two values: `STUDENT` and `ALUMNI`.

### Q22: What is the mentorship request lifecycle?

**A:** The mentorship request follows a 5-stage lifecycle:
1. **Student sends request** — Creates a `MentorshipRequest` with status `PENDING`, including topic and cover note.
2. **Alumni views requests** — Sees all incoming PENDING requests with student details.
3. **Alumni accepts** — Status changes to `ACCEPTED`, creating a mentorship connection.
4. **Alumni rejects** — Status changes to `REJECTED` (optional with reason).
5. **Connection established** — Both student and alumni can view their mentorship connections.

The `MentorshipStatus` enum: `PENDING`, `ACCEPTED`, `REJECTED`.

### Q23: How does alumni filtering work?

**A:** `AlumniFilterCriteria` supports multi-criteria filtering:
- Filter by **department** (exact match)
- Filter by **industry** (exact match)
- Filter by **minimum years of experience** (>= comparison)
- Multiple criteria can be combined — all conditions must match (AND logic)
- `AlumniDirectoryService.filterAlumni(AlumniFilterCriteria)` constructs dynamic SQL with `WHERE` clauses based on provided criteria.

---

## 7. Code Quality & Best Practices

### Q24: How do you handle validation?

**A:** `ValidationUtil` provides reusable validation methods:
- `isValidEmail(String)` — Regex-based email format validation
- `isValidPassword(String)` — Minimum length, complexity rules
- `isNonBlank(String)` — Null/empty/whitespace check
- `isValidYear(int)` — Range-based year validation
- Validation errors are returned as user-friendly messages, never raw exceptions.

### Q25: How do you handle exceptions?

**A:** The project uses structured exception handling:
- **Service layer** catches repository exceptions and wraps them in result DTOs (e.g., `LoginResult.failure("message")`).
- **UI layer** catches service exceptions and displays friendly error messages.
- **Database exceptions** are logged with `java.util.logging` but never expose SQL details, credentials, or stack traces to users.
- **`SecurityException`** is thrown for unauthorized access (caught by UI for display).
- Custom exception hierarchy for application-specific errors.

### Q26: What Java version is used and why?

**A:** Java 17 (LTS) — It provides:
- Modern language features like enhanced switch expressions, text blocks, sealed classes, and records.
- Long-term support for production stability.
- Improved performance and security patches.

### Q27: How is the project built?

**A:** Maven 3.9+ with the Maven Wrapper (`mvnw.cmd` / `mvnw`):
- `pom.xml` defines dependencies: MySQL Connector/J, JUnit 5, Maven Surefire Plugin.
- Build: `.\mvnw.cmd compile`
- Test: `.\mvnw.cmd test`
- Package: `.\mvnw.cmd package`
- Run: `.\mvnw.cmd compile exec:java`

---

## 8. Team Collaboration

### Q28: How did the team collaborate on this project?

**A:** Two-branch workflow using Git and GitHub:
- **Branch `anshika`** — Implemented student features, alumni features, mentorship workflows, UI polish, integration, and demo preparation.
- **Branch `chahana`** — Implemented role-based access control, comprehensive testing, and project documentation.
- Regular merges synchronized both branches while preserving each teammate's work.
- Feature-based commits with descriptive messages for traceability.

### Q29: How were merge conflicts handled?

**A:** Merge conflicts were resolved carefully by:
1. Inspecting conflicting files to understand both teammates' changes.
2. Preserving functionality from both branches.
3. Testing after every merge to ensure nothing broke.
4. Never using destructive commands like `git reset --hard` or force push.

### Q30: What is the Git commit history structure?

**A:** The project followed a prompt-based development approach:
- Prompts 1-9: Foundation, database, authentication, profiles, dashboards
- Prompts 10-11: Student and alumni dashboards
- Prompts 12-14: Alumni directory, search, and filtering
- Prompts 15-16: Alumni profile viewing and mentorship request sending
- Prompts 17-18: Alumni request management and student status tracking
- Prompt 19: Mentorship connections
- Prompt 20: Validation and exception handling
- Prompt 21: Role-based access control
- Prompt 22: Complete integration
- Prompt 23: Comprehensive testing
- Prompt 24: UI polish and user experience improvement
- Prompt 25: Project documentation
- Prompt 26: Final demo preparation and submission

---

## 9. Future Scope

### Q31: How would you extend AlumConnect in the future?

**A:**
- **Web UI:** Migrate from console to a Spring Boot web application with HTML/CSS/JS frontend.
- **Messaging:** Add real-time chat between mentors and mentees.
- **Notifications:** Email notifications for request status changes.
- **Admin Panel:** Administrative dashboard for managing users and monitoring platform activity.
- **Analytics:** Track mentorship engagement metrics, popular expertise areas, and connection success rates.
- **Event System:** Alumni-hosted workshops, webinars, and networking events.
- **Mobile App:** Android/iOS application for on-the-go mentorship management.

### Q32: What would you change about the design?

**A:**
- Consider using an ORM (Hibernate/JPA) for complex queries in a larger application.
- Implement JWT-based authentication for a web version.
- Add password reset and email verification features.
- Implement mentorship session scheduling and feedback.
- Add pagination for large alumni directories.

---

*Prepared for AlumConnect v1.0.0 — College Project Viva*
