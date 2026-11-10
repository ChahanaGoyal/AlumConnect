# AlumConnect — Academic Project Report

---

## 1. Cover Page

```text
================================================================================
                                PROJECT REPORT
                                      ON
                                 ALUMCONNECT
                 (Alumni Networking & Mentorship Platform)

          Submitted in partial fulfillment of the requirements for 
                 the award of the degree of Bachelor of Technology
================================================================================

SUBMITTED BY:
  [Student Name 1]                   Roll No: [Roll Number 1]
  [Student Name 2]                   Roll No: [Roll Number 2]

UNDER THE GUIDANCE OF:
  [Project Guide / Faculty Name]
  [Designation / Department]

INSTITUTION:
  [Department of Computer Science & Engineering]
  [Institute / University Name]
  [City, State, Country]

ACADEMIC YEAR:
  [202X – 202Y]
================================================================================
```

---

## 2. Abstract

In higher educational institutions, the professional accomplishments and real-world experience of graduated alumni represent an invaluable reservoir of career guidance, mentorship, and industry perspective for current students. However, traditional engagement channels—such as informal social media outreach, ad-hoc email communications, and sporadic annual reunion events—often suffer from low engagement, absence of role verification, security risks, and an inability to manage mentorship capacity effectively.

**AlumConnect** is an enterprise-grade, console-based Java networking and mentorship platform designed to formalize, streamline, and secure student-alumni interactions. Developed using Java 17, Apache Maven, and MySQL 8.0, AlumConnect implements a strict layered architecture comprising Presentation, Service, Data Access (pure JDBC with custom connection pooling), and Domain Model layers. Key capabilities include SHA-256 salted password hashing, robust role-based access control (RBAC), multi-criteria directory search, and a formal 5-stage mentorship request lifecycle (`PENDING` $\rightarrow$ `ACCEPTED` / `REJECTED`). Verified across an automated test suite of 540 test cases (with 515 passed unit/service tests and 25 graceful database-absence skips), AlumConnect provides institutions with a dependable, lightweight foundation for academic networking.

---

## 3. Introduction

Alumni-student mentorship plays a pivotal role in bridging the gap between theoretical classroom curricula and practical industry expectations. Guidance on resume formulation, technical interview strategies, domain transitions, and career trajectory mapping is most effectively delivered by individuals who have navigated identical institutional academic pathways.

Despite these mutual benefits, institutional alumni engagement has historically faced technical friction. Traditional alumni directories are static spreadsheets or passive database portals without interactive workflows. **AlumConnect** was conceived as an interactive, role-segregated terminal application providing dedicated, secure dashboards for students and alumni. By embedding verification, privacy, structured communication notes, and capacity limits into the system design, the platform transforms informal outreach into an organized, measurable engagement framework.

---

## 4. Problem Statement

Existing methods of student-to-alumni communication in educational environments suffer from several systemic shortcomings:
1. **Unstructured Communication:** Students frequently reach out via cold messaging on generic social media platforms without defined topics, leading to high alumni drop-off rates.
2. **Mentorship Burnout & Capacity Overflow:** Popular alumni receive disproportionate request volumes with no automated mechanisms to set capacity limits or indicate availability.
3. **Absence of Role Separation & Security Boundaries:** Public directories often expose personal phone numbers and email addresses indiscriminately, risking privacy breaches and harassment.
4. **Lack of Lifecycle Tracking:** In informal settings, students have no mechanism to track whether a request is under review, accepted, or declined with feedback.
5. **Technical Fragility:** Many educational portal prototypes suffer from weak input validation, vulnerability to SQL injection, and poor connection handling under concurrent usage.

---

## 5. Objectives

The primary objectives of the AlumConnect platform are:
1. **Role-Gated Dashboards:** Implement dedicated, intuitive console environments for Students and Alumni with strict access boundaries.
2. **Directory & Multi-Filter Search:** Provide students with real-time discovery of alumni mentors filtered by department, industry sector, and professional experience.
3. **Structured Mentorship Lifecycle:** Enforce a formal request lifecycle (`PENDING`, `ACCEPTED`, `REJECTED`) where alumni review student credentials and respond with structured guidance notes.
4. **Data Isolation & Security:** Safeguard user credentials using SHA-256 salted hashing and enforce horizontal data isolation (`requireUserId`) so no user can access or modify another user's records.
5. **Architectural Decoupling & Reliability:** Maintain a clean separation of concerns using pure JDBC, parameterized SQL queries, custom thread-safe connection pooling, and complete automated regression testing.

---

## 6. Scope of the Project

### In-Scope (Implemented Functionality):
* Console-based terminal application with standardized 50-character visual banners and sub-menu back navigation.
* Student and alumni account registration with email format checking, password strength rules, and duplicate detection.
* User authentication with active session tracking via an in-memory thread-safe singleton (`UserSession`).
* Complete student profile CRUD and alumni profile CRUD (including availability toggle and mentee capacity).
* Alumni directory browsing, keyword search, and multi-parameter filtering.
* Sending structured mentorship requests with validation against self-mentorship, alumni availability, and duplicate requests.
* Alumni request queue management (accepting and rejecting requests with feedback notes).
* Viewing confirmed mentorship connections and mutual contact information.
* Thread-safe JDBC connection pool with automated schema initialization.

### Out-of-Scope (Future Enhancements):
* Graphical User Interface (GUI) or web front-end (Spring Boot / React).
* Administrative user moderation portal (the `ADMIN` role is modeled in schema and enums, but has no UI controllers).
* Real-time peer-to-peer chat messaging or video conferencing integration.
* Job postings and internship application management.

---

## 7. Existing System

Traditional alumni systems in universities typically rely on:
* **Static PDF Directories or Spreadsheets:** Updated manually once per year; frequently contain outdated job titles and broken contact info.
* **Third-Party Professional Networks (e.g., LinkedIn):** Effective for public networking, but lack institution-specific verification, context, or structured student-mentor agreements.
* **Ad-hoc Emails:** Disorganized communication chains with no centralized record of mentorship outcomes.

### Drawbacks of Existing Systems:
* High friction and low response rates.
* Uncontrolled disclosure of personal contact details.
* No mechanism for alumni to limit incoming mentorship requests.
* Zero accountability or lifecycle state management.

---

## 8. Proposed System

**AlumConnect** replaces ad-hoc outreach with an institutional, role-based platform designed with the following advantages:
1. **Interactive Terminal Experience:** Lightweight, cross-platform CLI requiring zero heavy client installations or browser dependencies.
2. **Mentee Capacity Safeguards:** Alumni set a maximum mentee threshold (`max_mentees`, default: 3); once filled, the platform prevents additional incoming requests.
3. **Structured Request Context:** Students must state a specific mentorship topic and context, ensuring alumni receive clear, actionable inquiries.
4. **Defense-in-Depth Security:** Multi-layered security checks occur at the UI navigation layer, the service authorization layer, and the database parameter layer.
5. **High-Performance Architecture:** Pure JDBC connection pooling provides deterministic execution without the overhead of heavy ORM frameworks.

---

## 9. Technology Stack

* **Language:** Java 17 (LTS)
* **Build System:** Apache Maven 3.9.9 (via included Maven Wrapper `mvnw` / `mvnw.cmd`)
* **Database Management System:** MySQL 8.0+ Community Server (InnoDB Engine)
* **Database Driver:** MySQL Connector/J 8.0.33 (`com.mysql:mysql-connector-j`)
* **Testing Framework:** JUnit Jupiter 5.10.2 (`org.junit.jupiter:junit-jupiter`)
* **Test Runner:** Apache Maven Surefire Plugin 3.2.5
* **Architecture Pattern:** Layered Architecture (Presentation $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Database)
* **Cryptographic Hashing:** SHA-256 with cryptographically generated salt strings

---

## 10. System Requirements

### Hardware Requirements:
* **Processor:** x86_64 or ARM64 multi-core processor (1.5 GHz or faster).
* **RAM:** Minimum 2 GB (4 GB recommended).
* **Storage:** 200 MB free disk space for project build artifacts and dependencies.

### Software Requirements:
* **Operating System:** Windows 10/11, Ubuntu 20.04+, macOS 12+ (POSIX-compliant terminal).
* **Java Runtime:** JDK 17 or higher (tested on OpenJDK 17 and Oracle JDK 26).
* **Database:** MySQL Server 8.0 or later listening on `localhost:3306`.
* **Network:** Localhost TCP networking for JDBC socket communication.

---

## 11. System Architecture

The AlumConnect codebase is organized into four distinct tiers:

```text
+-------------------------------------------------------------+
|                  Presentation Layer (UI)                    |
|  AlumConnectApp, AuthUI, RegistrationUI,                    |
|  StudentDashboardUI, AlumniDashboardUI,                     |
|  StudentProfileUI, AlumniProfileUI, AlumniDirectoryUI,      |
|  ConsoleFormatter                                           |
+-------------------------------------------------------------+
                             |
                             v
+-------------------------------------------------------------+
|                      Service Layer                          |
|  UserService, StudentProfileService,                        |
|  AlumniProfileService, AlumniDirectoryService,              |
|  MentorshipRequestService, DTOs                             |
+-------------------------------------------------------------+
                             |
                             v
+-------------------------------------------------------------+
|              Data Access Layer (Repositories)               |
|  UserRepository, StudentProfileRepository,                  |
|  AlumniProfileRepository, MentorshipRequestRepository       |
|  (Jdbc* implementations using PreparedStatement)            |
+-------------------------------------------------------------+
                             |
                             v
+-------------------------------------------------------------+
|             Database & Infrastructure Tier                  |
|  ConnectionPool, PooledConnectionWrapper,                   |
|  DatabaseConnection, DatabaseInitializer, MySQL 8.0 (InnoDB)|
+-------------------------------------------------------------+
```

### Communication Flow:
1. User input is captured by console scanners in the **Presentation Layer** and packed into Data Transfer Objects (`DTO`).
2. The **Service Layer** evaluates business invariants, checks the active `UserSession`, and hashes passwords.
3. The **Repository Layer** acquires a pooled connection, executes parameterized SQL queries (`?`), and maps rows to domain POJOs.
4. Results propagate back to the UI formatted with `ConsoleFormatter`.

---

## 12. Database Design

The relational database (`alumconnect_db`) consists of four normalized tables enforcing ACID compliance:

### 12.1. Table: `users`
* `id` (`INT AUTO_INCREMENT PRIMARY KEY`)
* `email` (`VARCHAR(150) NOT NULL UNIQUE`)
* `password_hash` (`VARCHAR(255) NOT NULL`)
* `full_name` (`VARCHAR(100) NOT NULL`)
* `role` (`ENUM('STUDENT', 'ALUMNI', 'ADMIN') NOT NULL`)
* `phone` (`VARCHAR(20) DEFAULT NULL`)
* `is_active` (`BOOLEAN DEFAULT TRUE`)
* `created_at` / `updated_at` (`TIMESTAMP`)
* *Constraints:* `chk_email_format CHECK (email LIKE '%_@__%.__%')`

### 12.2. Table: `student_profiles`
* `id` (`INT AUTO_INCREMENT PRIMARY KEY`)
* `user_id` (`INT NOT NULL UNIQUE`, FK to `users(id)` ON DELETE CASCADE)
* `enrollment_number` (`VARCHAR(50) UNIQUE DEFAULT NULL`)
* `degree` (`VARCHAR(100) DEFAULT 'Bachelor of Technology'`)
* `department` (`VARCHAR(100) NOT NULL`)
* `graduation_year` (`INT NOT NULL`, Check 1950..2100)
* `current_semester` (`INT DEFAULT 1`, Check 1..12)
* `bio`, `skills`, `interests` (`TEXT`)
* `linkedin_url`, `github_url` (`VARCHAR(255)`)

### 12.3. Table: `alumni_profiles`
* `id` (`INT AUTO_INCREMENT PRIMARY KEY`)
* `user_id` (`INT NOT NULL UNIQUE`, FK to `users(id)` ON DELETE CASCADE)
* `graduation_year` (`INT NOT NULL`, Check 1950..2100)
* `degree` (`VARCHAR(100) DEFAULT 'Bachelor of Technology'`)
* `department` (`VARCHAR(100) NOT NULL`)
* `current_company`, `current_role`, `industry` (`VARCHAR`)
* `years_of_experience` (`INT DEFAULT 0`, Check $\ge 0$)
* `expertise_areas`, `bio` (`TEXT`)
* `linkedin_url` (`VARCHAR(255)`)
* `is_available_for_mentorship` (`BOOLEAN DEFAULT TRUE`)
* `max_mentees` (`INT DEFAULT 3`, Check $\ge 0$)

### 12.4. Table: `mentorship_requests`
* `id` (`INT AUTO_INCREMENT PRIMARY KEY`)
* `student_id` (`INT NOT NULL`, FK to `users(id)` ON DELETE CASCADE)
* `alumni_id` (`INT NOT NULL`, FK to `users(id)` ON DELETE CASCADE)
* `topic` (`VARCHAR(150) NOT NULL`)
* `message` (`TEXT NOT NULL`)
* `status` (`ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING'`)
* `response_notes` (`TEXT DEFAULT NULL`)
* `requested_at`, `responded_at`, `completed_at`, `updated_at` (`TIMESTAMP`)
* *Constraints:* `chk_distinct_participants CHECK (student_id <> alumni_id)`

---

## 13. Module Descriptions

| Module | Core Classes | Primary Functionality |
| :--- | :--- | :--- |
| **Authentication & Session** | `UserService`, `UserSession`, `PasswordUtil`, `AuthUI` | Registration, credential validation, salted password hashing, in-memory session tracking, safe logout. |
| **Student Management** | `StudentProfileService`, `StudentDashboardUI`, `StudentProfileUI` | Profile creation/updating, academic record maintenance, semester boundaries. |
| **Alumni Management** | `AlumniProfileService`, `AlumniDashboardUI`, `AlumniProfileUI` | Career profile maintenance, mentorship availability toggling, mentee capacity management. |
| **Directory & Discovery** | `AlumniDirectoryService`, `AlumniDirectoryUI` | Directory listing, free-text search, structured filtering by department/industry/experience. |
| **Mentorship Lifecycle** | `MentorshipRequestService`, `JdbcMentorshipRequestRepository` | Request creation, duplicate prevention, alumni inbox management, acceptance/rejection, connections listing. |
| **Console Presentation** | `ConsoleFormatter`, `AlumConnectApp` | 50-character visual banners, borders, status tags, sub-menu return flows, input parsing. |
| **Database Infrastructure**| `ConnectionPool`, `PooledConnectionWrapper`, `DatabaseInitializer` | Thread-safe connection pool, connection recycling, automated DDL execution on startup. |

---

## 14. Implementation Details

1. **Security & Cryptography:** Password hashing uses SHA-256 with an 8-byte hexadecimal salt (`PasswordUtil`). Raw passwords are never held in domain objects, session structures, or log messages.
2. **Defensive JDBC Access:** Repositories use `PreparedStatement` with `?` parameter markers exclusively. Connections and statements are bound within `try-with-resources` blocks to ensure immediate resource reclamation.
3. **Transaction Safety:** Multi-table operations (e.g. user creation followed by profile record generation) execute within a single transaction; if profile creation fails, `connection.rollback()` is invoked to prevent orphaned account records.
4. **Ownership Validation:** Horizontal access control is enforced in Java services before executing SQL updates (`UserSession.getInstance().requireUserId(targetId)`), preventing IDOR (Insecure Direct Object Reference) vulnerabilities.

---

## 15. Testing and Results

The platform was subjected to extensive automated regression and integration testing using JUnit 5 and the Maven Surefire Plugin:

### Summary of Surefire Test Execution:
* **Total Tests Executed:** **540**
* **Passed Tests:** **515**
* **Failures:** **0**
* **Errors:** **0**
* **Skipped Tests:** **25** (pre-existing live MySQL integration tests that gracefully skip when MySQL is offline via `Assumptions.assumeTrue(...)`)
* **Build Outcome:** **`BUILD SUCCESS`** in 6.480 s

### Key Tested Areas:
* Unit verification of password hashing and input validation rules (`PasswordUtilTest`, `ValidationUtilTest`).
* Service-layer authorization and RBAC boundaries (`RoleBasedAccessControlTest`, `AccessControlAndValidationDeepTest`).
* Complete 5-stage mentorship request lifecycle workflows (`AlumniAndMentorshipWorkflowTest`).
* Console UI input parsing, navigation sub-menus, and cancellation paths (`UIPolishAndExperienceTest`).
* Dynamic proxy tests validating SQL parameterization and transaction rollback under simulated network faults (`RepositoryAndDatabaseDeepTest`).

---

## 16. Limitations

To ensure strict academic accuracy, the following system limitations are acknowledged:
1. **Console Terminal Interface Only:** The current application operates via CLI; no graphical user interface (GUI) or web interface exists.
2. **No Live Push Notifications:** Students must manually log in and check their requests dashboard to see alumni feedback notes.
3. **Unimplemented Admin Console:** Although the `ADMIN` role is defined in the schema and Java models, administrative moderation menus and services have not yet been developed.
4. **Local Network Scope:** The application is configured for a single institution database instance on `localhost:3306`; multi-tenant institutional partitioning is not yet implemented.

---

## 17. Future Enhancements

1. **Web and Mobile Client:** Development of a RESTful Spring Boot API and React-based web dashboard to provide responsive mobile and browser access.
2. **Real-time Messaging:** Integration of WebSocket or in-app messaging to allow mentors and accepted mentees to chat and share documents directly.
3. **Administrative Moderation Suite:** Web portal for college administrators to approve registered alumni credentials, manage institutional departments, and review engagement analytics.
4. **Career & Internship Board:** Dedicated module where verified alumni can post internships and job openings directly for students to submit resumes.

---

## 18. Conclusion

**AlumConnect** successfully addresses the challenge of unorganized student-alumni networking in higher educational institutions. By combining a clean layered architecture with pure JDBC connection pooling, robust role-based access control, SHA-256 password protection, and structured mentorship lifecycles, the platform establishes an effective, dependable, and verifiable communication bridge between university students and alumni mentors.

---

## 19. References

1. Oracle Corporation. *Java SE 17 Documentation & Language Specification*. https://docs.oracle.com/en/java/javase/17/
2. MySQL AB / Oracle Corporation. *MySQL 8.0 Reference Manual: InnoDB Storage Engine*. https://dev.mysql.com/doc/refman/8.0/en/
3. JUnit Team. *JUnit 5 User Guide*. https://junit.org/junit5/docs/current/user-guide/
4. Apache Software Foundation. *Apache Maven Surefire Plugin Documentation*. https://maven.apache.org/surefire/
5. OWASP Foundation. *OWASP Top 10: Insecure Direct Object References (IDOR) & Broken Access Control*. https://owasp.org/
