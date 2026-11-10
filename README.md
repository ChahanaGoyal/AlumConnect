# AlumConnect — Alumni Networking & Mentorship Platform

> A robust, role-based Java console platform connecting university students with alumni for career guidance, professional networking, and structured mentorship.

---

## 1. Project Overview

**AlumConnect** is an enterprise-grade, console-based Java application built to foster professional connections and structured mentorship between enrolled university students and graduated alumni. The system incorporates strict role-based access control (RBAC), multi-criteria directory search, a complete 5-stage mentorship request lifecycle, SHA-256 salted password security, and high-performance JDBC connection pooling against MySQL.

---

## 2. Problem Statement

In academic institutions, students frequently lack direct, structured channels to seek career guidance, resume reviews, industry insights, and interview preparation from experienced alumni. Concurrently, alumni willing to mentor students often lack an organized platform to manage their mentorship bandwidth, screen incoming requests, and define their areas of expertise. Unstructured communication channels (e.g., cold outreach on general social media) suffer from high friction, privacy concerns, lack of accountability, and zero institution-aligned role verification.

---

## 3. Project Objectives

1. **Structured Mentorship Lifecycles:** Facilitate formal student-to-alumni mentorship requests with clear request topics, cover notes, acceptance/rejection workflows, and mutual connection records.
2. **Dynamic Alumni Discovery:** Enable students to browse and filter verified alumni by department, industry, and years of professional experience.
3. **Data Security & Privacy:** Implement SHA-256 salted password hashing, credential masking, defense-in-depth role-based access control, and complete profile isolation.
4. **Architectural Excellence:** Maintain a clean layered architecture (Model-Repository-Service-UI) with pure JDBC, thread-safe connection pooling, and 100% test coverage across core domain workflows.
5. **Intuitive Terminal Experience:** Deliver a polished, consistent 50-character console user interface with clear visual hierarchy, sub-menu back navigation, and graceful exit handling.

---

## 4. Main Implemented Features

| Feature Domain | Implemented Functionality |
| :--- | :--- |
| **Authentication & Sessions** | Role-based registration (`STUDENT` / `ALUMNI`), login with credential validation, in-memory singleton session management (`UserSession`), safe logout, and credential masking. |
| **Student Profiles** | Profile creation and editing (degree, department, graduation year, semester, enrollment number, bio, skills, interests, LinkedIn, GitHub). |
| **Alumni Profiles** | Profile creation and editing (company, role, industry, years of experience, expertise areas, LinkedIn, mentorship availability toggle, maximum mentees capacity). |
| **Alumni Directory** | Real-time directory browsing, name/keyword search, and multi-parameter filtering (by department, industry, and minimum experience threshold) with friendly empty states. |
| **Mentorship Requests** | Students send structured requests (topic, message); alumni inspect incoming requests, view student academic details, and accept or reject with optional notes. |
| **Mentorship Connections** | Dedicated screen for students and alumni to view all mutually confirmed mentorship connections and contact details. |
| **Access Control (RBAC)** | Role enforcement at both UI layer and Service layer; horizontal privacy boundaries prevent any user from viewing or modifying another user's private data. |
| **Infrastructure & Reliability** | Custom thread-safe JDBC connection pool (`ConnectionPool`), automatic schema initialization (`DatabaseInitializer`), and health checks (`DatabaseHealthCheck`). |

---

## 5. Technologies Used

* **Programming Language:** Java 17 (LTS)
* **Build Tool:** Apache Maven 3.9+ (configured via Maven Wrapper `mvnw` / `mvnw.cmd`)
* **Database Driver:** MySQL Connector/J `8.0.33` (`com.mysql:mysql-connector-j`)
* **Database Engine:** MySQL 8.0+ (InnoDB Storage Engine, UTF-8 / `utf8mb4`)
* **Testing Framework:** JUnit Jupiter 5.10.2 (`org.junit.jupiter:junit-jupiter`)
* **Surefire Test Runner:** Apache Maven Surefire Plugin `3.2.5`
* **User Interface:** Java Console CLI (ANSI / ASCII terminal layout formatted via `ConsoleFormatter`)

---

## 6. System Requirements

* **Java Development Kit (JDK):** JDK 17 or higher (tested on OpenJDK 17 and Oracle JDK 26).
* **Build Tool:** Maven Wrapper is included in the repository (`mvnw` / `mvnw.cmd`). Alternatively, Maven 3.8+ installed locally.
* **Database:** MySQL Server 8.0 or later running on `localhost:3306`.
* **Operating System:** Windows 10/11, macOS, or Linux.

---

## 7. Actual Project Architecture

The application adheres to a clean **Layered Architecture (Separation of Concerns)**:

```text
+-------------------------------------------------------+
|                 Presentation Layer (UI)               |
|  AlumConnectApp, AuthUI, RegistrationUI,              |
|  StudentDashboardUI, AlumniDashboardUI,               |
|  StudentProfileUI, AlumniProfileUI, AlumniDirectoryUI |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                   Service Layer                       |
|  UserService, StudentProfileService,                  |
|  AlumniProfileService, AlumniDirectoryService,        |
|  MentorshipRequestService                             |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|             Data Access Layer (Repository)            |
|  UserRepository, StudentProfileRepository,            |
|  AlumniProfileRepository, MentorshipRequestRepository |
|  (backed by Jdbc* implementations & PreparedStatement)|
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|               Infrastructure & Database               |
|  ConnectionPool, PooledConnectionWrapper,             |
|  DatabaseConnection, DatabaseInitializer, MySQL 8.0   |
+-------------------------------------------------------+
```

1. **Presentation Layer (`com.alumconnect.ui`):** Handles console menus, user input capture via `Scanner`, formatting via `ConsoleFormatter`, and input delegation.
2. **Service Layer (`com.alumconnect.service`):** Enforces business rules, RBAC authorization, password hashing, and session validation using Data Transfer Objects (`dto`).
3. **Repository Layer (`com.alumconnect.repository`):** Executes parameterized SQL queries (`?`) using `PreparedStatement` within `try-with-resources` blocks to guard against SQL injection and resource leaks.
4. **Model Layer (`com.alumconnect.model`):** Pure domain POJOs (`User`, `Student`, `Alumni`, `MentorshipRequest`) mirroring database entities.
5. **Utility Layer (`com.alumconnect.util`):** Cross-cutting concerns including `ConnectionPool`, `UserSession`, `PasswordUtil`, and `ValidationUtil`.

---

## 8. Folder and Package Structure

```text
AlumConnect/
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
├── schema.sql
├── docs/
│   └── DATABASE_DESIGN.md
└── src/
    ├── main/
    │   ├── java/com/alumconnect/
    │   │   ├── AlumConnectApp.java
    │   │   ├── model/
    │   │   │   ├── Alumni.java
    │   │   │   ├── MentorshipRequest.java
    │   │   │   ├── MentorshipStatus.java
    │   │   │   ├── Role.java
    │   │   │   ├── Student.java
    │   │   │   └── User.java
    │   │   ├── repository/
    │   │   │   ├── AlumniProfileRepository.java
    │   │   │   ├── JdbcAlumniProfileRepository.java
    │   │   │   ├── JdbcMentorshipRequestRepository.java
    │   │   │   ├── JdbcStudentProfileRepository.java
    │   │   │   ├── JdbcUserRepository.java
    │   │   │   ├── MentorshipRequestRepository.java
    │   │   │   ├── StudentProfileRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── service/
    │   │   │   ├── AlumniDirectoryService.java
    │   │   │   ├── AlumniProfileService.java
    │   │   │   ├── MentorshipRequestService.java
    │   │   │   ├── StudentProfileService.java
    │   │   │   ├── UserService.java
    │   │   │   └── dto/
    │   │   ├── ui/
    │   │   │   ├── AlumniDashboardUI.java
    │   │   │   ├── AlumniDirectoryUI.java
    │   │   │   ├── AlumniProfileUI.java
    │   │   │   ├── AuthUI.java
    │   │   │   ├── ConsoleFormatter.java
    │   │   │   ├── RegistrationUI.java
    │   │   │   ├── StudentDashboardUI.java
    │   │   │   └── StudentProfileUI.java
    │   │   └── util/
    │   │       ├── ConnectionPool.java
    │   │       ├── DatabaseConnection.java
    │   │       ├── DatabaseHealthCheck.java
    │   │       ├── DatabaseInitializer.java
    │   │       ├── PasswordUtil.java
    │   │       ├── PooledConnectionWrapper.java
    │   │       ├── UserSession.java
    │   │       └── ValidationUtil.java
    │   └── resources/
    │       ├── application.properties
    │       └── schema.sql
    └── test/
        └── java/com/alumconnect/
            ├── AlumConnectAppTest.java
            ├── model/
            ├── repository/
            ├── schema/
            ├── service/
            ├── ui/
            └── util/
```

---

## 9. Database Setup Instructions

1. Start your local MySQL service (e.g. via MySQL Workbench, Windows Services, or terminal):
   ```bash
   # On Windows (Admin Command Prompt)
   net start MySQL80

   # On Linux / macOS
   sudo systemctl start mysql
   ```
2. Log in to MySQL as root or an administrative user:
   ```bash
   mysql -u root -p
   ```
3. Run the project DDL schema:
   ```sql
   SOURCE schema.sql;
   ```
   *(Alternatively, `DatabaseInitializer` will automatically create the database and tables on application startup if the configured user has DDL privileges).*

---

## 10. MySQL Configuration Instructions

Database credentials are read from [`src/main/resources/application.properties`](file:///c:/coding/AlumConnect/AlumConnect/src/main/resources/application.properties):

```properties
db.url=jdbc:mysql://localhost:3306/alumconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.server.url=jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_mysql_password
db.driver=com.mysql.cj.jdbc.Driver

# Connection Pool Settings
db.pool.maxSize=10
db.pool.minIdle=2
db.pool.connectionTimeoutMs=3000
db.pool.validationQuery=SELECT 1
```

> [!TIP]
> **Environment Variable Overrides:**
> You can override database credentials without modifying files by setting environment variables:
> `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `DB_DRIVER`.
> Alternatively, pass Java system properties at runtime:
> `-Ddb.url=... -Ddb.user=... -Ddb.password=...`

---

## 11. Build and Run Instructions

### Building the Project
To compile source code and package the executable JAR:
```bash
# On Windows
.\mvnw.cmd clean package

# On Linux / macOS
./mvnw clean package
```

### Running the Application
You can run the application directly through Maven or via the packaged JAR:

**Option 1: Using Maven Exec Plugin**
```bash
# On Windows
.\mvnw.cmd compile exec:java

# On Linux / macOS
./mvnw compile exec:java
```

**Option 2: Running the Executable JAR**
```bash
java -jar target/alumconnect-1.0-SNAPSHOT.jar
```

---

## 12. Registration and Login Instructions

1. **Launch the Application:** The main menu presents three options:
   ```text
   ==================================================
                      ALUMCONNECT
               Alumni Networking Platform
   ==================================================
                       MAIN MENU
   --------------------------------------------------
   Welcome to AlumConnect (v1.0.0)!
   --------------------------------------------------
   Main Options:
     [1] Login (Student or Alumni)
     [2] Register New Account
     [0] Exit
   --------------------------------------------------
   Enter your choice:
   ```
2. **Registration (`[2]`):**
   * Select your role: `1` for **Student** or `2` for **Alumni**.
   * Enter full name, unique email address, password (minimum 8 characters, with letters and numbers), and optional phone number.
   * Provide initial profile information (Department, Graduation Year, Degree).
3. **Login (`[1]`):**
   * Enter your registered email and password (or enter `0` to cancel and return to the main menu).
   * Upon successful authentication, `UserSession` is initialized and you are routed to your role-specific dashboard.

---

## 13. Student Workflow

Once logged in as a Student, the **Student Dashboard** provides:
* **`[1] View My Profile` / `[2] Update My Profile`:** Inspect and update degree, department, current semester, bio, skills, interests, LinkedIn, and GitHub links.
* **`[3] Browse Alumni Directory`:**
  * View all verified alumni with company and role information.
  * Search alumni by name or keyword.
  * Filter alumni by department, industry, and minimum years of experience.
* **`[4] Send Mentorship Request`:**
  * Select an alumnus ID from the directory.
  * Provide a clear mentorship topic (e.g. "Mock System Design Interview") and message.
* **`[5] View Sent Mentorship Requests`:** Review real-time status of your requests (`PENDING`, `ACCEPTED`, `REJECTED`) and alumni feedback notes.
* **`[6] View Mentorship Connections`:** View confirmed alumni mentors with email and LinkedIn contact information.
* **`[7] Logout`:** Safely clears session state and returns to the main menu.

---

## 14. Alumni Workflow

Once logged in as an Alumnus, the **Alumni Dashboard** provides:
* **`[1] View My Profile` / `[2] Update My Profile`:** Maintain current company, job role, industry, years of experience, expertise areas, bio, LinkedIn profile, mentorship availability toggle, and maximum mentee capacity.
* **`[3] View Incoming Mentorship Requests`:** Inspect pending requests from students, including student degree, department, graduation year, and request notes.
* **`[4] Accept Mentorship Request`:** Approve a pending request and attach optional notes (e.g., meeting schedule or communication preferences).
* **`[5] Reject Mentorship Request`:** Decline a request with polite feedback notes (bandwidth full, out of domain, etc.).
* **`[6] View Mentorship Connections`:** View all active student mentees.
* **`[7] Logout`:** Safely invalidates the active session.

---

## 15. Mentorship Request Lifecycle

The platform models a strict 5-stage request lifecycle:

```text
[ Student Dispatches Request ]
              |
              v
       ( PENDING ) -----------------------------------+
              |                                        |
       Alumni Reviews                                  | Alumni Declines
              |                                        |
              v                                        v
       ( ACCEPTED )                              ( REJECTED )
              |                                        |
       Mutual Connection                        No Connection Formed
       Established for Both                            |
              |                                        v
   ( COMPLETED / CANCELLED ) <-------------------------+
   (Schema-ready transitions)
```

* **Constraints Enforced:**
  * A student cannot send a mentorship request to themselves (`student_id <> alumni_id`).
  * Duplicate pending requests to the same alumnus are prevented.
  * Inactive alumni or alumni who have toggled mentorship availability off cannot receive new requests.
  * Only the targeted alumnus can accept or reject a request.
  * Rejected and pending requests do not appear in the active connections roster.

---

## 16. Role-Based Access Control (RBAC)

AlumConnect employs **Defense-in-Depth** for security:
1. **UI Presentation Guard:** Dashboard menus exclusively display options authorized for the logged-in user's role.
2. **Service Layer Guard:** Every business service independently checks `UserSession.getInstance().getCurrentUser()` and enforces role preconditions. A student directly invoking alumni methods receives an `AccessDeniedException`.
3. **Horizontal Separation:** Users cannot view or modify profiles or request histories belonging to another user.
4. **Session Invalidation:** Logging out clears the singleton session reference; unauthenticated attempts to access protected services fail immediately.

---

## 17. Testing Instructions

The project features a comprehensive test suite of **540 tests** covering unit logic, JDBC contracts, role-based access control, input validation, UI navigation, and workflow lifecycles.

### Running the Test Suite
```bash
# Execute all tests
.\mvnw.cmd test

# Run a specific test class
.\mvnw.cmd test -Dtest=AuthenticationWorkflowTest
```

### Test Suite Structure
* `com.alumconnect.service`: Workflow and access control tests (`AuthenticationWorkflowTest`, `StudentWorkflowTest`, `AlumniAndMentorshipWorkflowTest`, `AccessControlAndValidationDeepTest`, `RoleBasedAccessControlTest`, `ValidationAndExceptionHandlingTest`).
* `com.alumconnect.repository`: Dynamic proxy JDBC contract tests (`RepositoryAndDatabaseDeepTest`) verifying SQL parameterization, transaction rollbacks, and ownership constraints.
* `com.alumconnect.ui`: Terminal UI rendering, navigation loops, and cancel/back options (`UIPolishAndExperienceTest`, `StudentDashboardUITest`, `AlumniDashboardUITest`).
* `com.alumconnect.util`: Connection pool verification, health check, password hashing (`DatabaseConnectionTest`, `PasswordUtilTest`, `ValidationUtilTest`).

> [!NOTE]
> When MySQL is not running on `localhost:3306`, the 25 live-database integration tests gracefully skip via JUnit 5 `Assumptions.assumeTrue(...)`. All 515 unit, mock, and proxy tests execute and pass unconditionally.

---

## 18. Team Contribution Overview

Contributions verified from the project repository commit history:

| Team Member | Git Identity & Branch | Key Areas of Contribution |
| :--- | :--- | :--- |
| **Chahana Goyal** | `ChahanaGoyal` (`chahana`) | Role-Based Access Control (RBAC), Mentorship Connections, Alumni Mentorship Request Management, Alumni Profile & Search, Profile Persistence, Comprehensive Regression & Integration Test Suites (Prompts 21, 23A–23F). |
| **Anshika** | `anshika` (`anshika`) | Console UI Polish & User Experience (`ConsoleFormatter`, sub-menu back navigation, cancel flows), Validation & Exception Handling, Student Mentorship Status, Directory Search & Filtering, Student Profile & Dashboard, Registration & Authentication (Prompts 14–20, 22, 24). |

---

## 19. Future Scope

While the core networking and mentorship foundation is complete, future enhancements may include:
* **Admin Management Console:** Admin UI for managing verified users, reviewing flagged profiles, and auditing platform activity (`Role.ADMIN` is already provisioned in the database schema).
* **Internship & Job Board:** Structured postings where alumni can share job opportunities and student members can submit resumes.
* **In-App Messaging:** Real-time messaging or scheduled meeting integrations between connected mentors and mentees.
* **Graphical User Interface (GUI):** A modern Web interface (Spring Boot + React/Vue) or cross-platform desktop UI to complement the terminal CLI.

---

## 20. Conclusion

**AlumConnect** bridges the gap between academic education and industry careers by providing university students and alumni with a structured, secure, and intuitive mentorship ecosystem. Through clean architecture, defensive programming, automated test coverage, and clear separation of responsibilities, AlumConnect establishes a reliable foundation for institutional alumni networking.
