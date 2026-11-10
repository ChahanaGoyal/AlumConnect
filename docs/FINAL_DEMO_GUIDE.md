# AlumConnect — Final Demo Guide

> Step-by-step walkthrough for demonstrating all features of the **AlumConnect** Alumni Networking & Mentorship Platform during the college project viva/demo.

---

## Part A — Prerequisites & Setup

### A.1 Environment Requirements

| Component        | Requirement                    |
|------------------|-------------------------------|
| **Java**         | JDK 17 or later               |
| **Database**     | MySQL 8.0 (running locally)   |
| **Build Tool**   | Maven 3.9+ (wrapper included) |
| **Terminal**     | Windows CMD / PowerShell      |

### A.2 Database Setup

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create the database
CREATE DATABASE IF NOT EXISTS alumconnect_db;

-- Create the application user
CREATE USER IF NOT EXISTS 'alumconnect_user'@'localhost' IDENTIFIED BY 'alumconnect_pass';
GRANT ALL PRIVILEGES ON alumconnect_db.* TO 'alumconnect_user'@'localhost';
FLUSH PRIVILEGES;
```

> **Note:** The application auto-creates all required tables on first startup via `DatabaseInitializer`.

### A.3 Configure Database Connection

Edit `src/main/resources/database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/alumconnect_db
db.username=alumconnect_user
db.password=alumconnect_pass
db.pool.size=5
```

### A.4 Build & Compile

```bash
.\mvnw.cmd clean compile
```

### A.5 Run All Tests

```bash
.\mvnw.cmd test
```

Expected: **540 tests, 0 failures, 0 errors** (25 skipped without a live database).

### A.6 Launch the Application

```bash
.\mvnw.cmd compile exec:java
```

---

## Part B — Authentication Demo

### B.1 Student Registration

1. At the **Main Menu**, enter `2` (Register New Account).
2. Select `1` (Student).
3. Enter sample data:
   - **Full Name:** `Aarav Sharma`
   - **Email:** `aarav.student@alumconnect.test`
   - **Password:** `SecurePass@123`
   - **Department:** `Computer Science`
4. Observe: success message with hashed password confirmation (password is never displayed in plaintext).

### B.2 Alumni Registration

1. At the **Main Menu**, enter `2` (Register New Account).
2. Select `2` (Alumni).
3. Enter sample data:
   - **Full Name:** `Priya Verma`
   - **Email:** `priya.alumni@alumconnect.test`
   - **Password:** `AlumniPass@456`
   - **Department:** `Information Technology`
4. Observe: success message.

### B.3 Student Login

1. At the **Main Menu**, enter `1` (Login).
2. Enter:
   - **Email:** `aarav.student@alumconnect.test`
   - **Password:** `SecurePass@123`
3. Observe: authentication success → routed to **Student Dashboard**.

### B.4 Invalid Login

1. At the **Main Menu**, enter `1` (Login).
2. Enter:
   - **Email:** `aarav.student@alumconnect.test`
   - **Password:** `WrongPassword`
3. Observe: friendly error message — `"Invalid email or password"`. No stack traces exposed.

### B.5 Logout

1. From any dashboard, enter `9` (Logout).
2. Observe: session cleared, returned to Main Menu.
3. Verify: cannot access dashboard features without re-logging in.

---

## Part C — Student Workflow Demo

### C.1 Student Profile Management

1. Login as `aarav.student@alumconnect.test`.
2. From the **Student Dashboard**, enter `1` (My Profile).
3. **Create Profile:**
   - Enrollment Number: `CS2024001`
   - Current Semester: `6`
   - Graduation Year: `2026`
   - Skills: `Java, Python, Machine Learning`
4. **View Profile:** Select the view option — verify all data appears correctly.
5. **Update Profile:** Change skills to `Java, Python, Machine Learning, Cloud Computing` — verify update saved.

### C.2 Browse Alumni Directory

1. From the **Student Dashboard**, enter `2` (Browse Alumni).
2. Observe: list of registered alumni with names, departments, industries, and mentorship status.

### C.3 Search Alumni

1. From the **Student Dashboard**, enter `3` (Search Alumni).
2. Search by keyword: `Priya`
3. Observe: matching alumni displayed with profile summaries.

### C.4 Filter Alumni

1. From the **Student Dashboard**, enter `4` (Filter Alumni).
2. Filter by:
   - **Department:** `Information Technology`
   - **Industry:** (leave blank)
   - **Years of Experience:** (leave blank)
3. Observe: filtered list of alumni matching criteria.

### C.5 View Alumni Profile & Send Mentorship Request

1. From the alumni listing (Browse/Search/Filter), select an alumni to view their full profile.
2. Observe: complete alumni profile with expertise areas, mentorship status, and professional summary.
3. Select the option to **Send Mentorship Request**.
4. Enter:
   - **Topic:** `Career Guidance in Software Engineering`
   - **Cover Note:** `I am interested in learning about your experience in the industry and would appreciate guidance on my career path.`
5. Observe: request sent successfully.

### C.6 View My Mentorship Requests

1. From the **Student Dashboard**, enter `5` (My Mentorship Requests).
2. Observe: list of all sent requests with their current status (PENDING, ACCEPTED, REJECTED).
3. Verify the request sent in C.5 appears with status `PENDING`.

### C.7 View Mentorship Connections

1. From the **Student Dashboard**, enter `6` (My Mentorship Connections).
2. Observe: list of accepted mentorship connections (will show connections after alumni accepts requests).

---

## Part D — Alumni Workflow Demo

### D.1 Alumni Login & Profile Management

1. Login as `priya.alumni@alumconnect.test`.
2. From the **Alumni Dashboard**, enter `1` (My Profile).
3. **Create Profile:**
   - Current Company: `Google India`
   - Current Role: `Senior Software Engineer`
   - Industry: `Technology`
   - Years of Experience: `8`
   - Expertise Areas: `Cloud Computing, System Design, Machine Learning`
   - LinkedIn URL: `https://linkedin.com/in/priyaverma`
   - Mentorship Status: `ACTIVE`
   - Max Mentees: `5`
4. **View Profile:** Verify all data appears correctly.
5. **Update Profile:** Change max mentees to `3` — verify update saved.

### D.2 View Incoming Mentorship Requests

1. From the **Alumni Dashboard**, enter `2` (View Mentorship Requests).
2. Observe: list of incoming requests from students.
3. Verify the request from `Aarav Sharma` appears with topic and cover note.

### D.3 Accept a Mentorship Request

1. From the request list, select the request from `Aarav Sharma`.
2. Choose **Accept**.
3. Observe: request status changes to `ACCEPTED`.

### D.4 View My Mentees (Connections)

1. From the **Alumni Dashboard**, enter `3` (My Mentees).
2. Observe: `Aarav Sharma` now appears as an active mentee.

### D.5 Reject a Request (if available)

1. If additional requests exist, select one and choose **Reject**.
2. Observe: status changes to `REJECTED`.

---

## Part E — Cross-Feature Verification

### E.1 Student Sees Accepted Connection

1. Login as `aarav.student@alumconnect.test`.
2. Go to **My Mentorship Connections** (option 6).
3. Observe: `Priya Verma` now appears as a mentor connection (status ACCEPTED).

### E.2 Role-Based Access Control Verification

- Students **cannot** access alumni dashboard features.
- Alumni **cannot** access student dashboard features.
- Unauthenticated users **cannot** access any dashboard.
- `UserSession.requireRole()` enforces access control at every entry point.

### E.3 Security Verification

- Passwords are **never displayed** in any output.
- Passwords are stored as **SHA-256 salted hashes**.
- Users can only view/modify **their own** profile data.
- `requireUserId()` prevents cross-user data access.

### E.4 Input Validation

- Invalid emails are rejected during registration.
- Empty or blank required fields are rejected.
- Duplicate email registration is rejected.
- Non-numeric menu inputs display friendly error messages.

---

## Part F — Architecture Highlights (for Viva Discussion)

### Layered Architecture

```
┌───────────────────────────────────────────────┐
│  Presentation Layer (com.alumconnect.ui)      │
│  AlumConnectApp, AuthUI, RegistrationUI,      │
│  StudentDashboardUI, AlumniDashboardUI,       │
│  StudentProfileUI, AlumniProfileUI,           │
│  AlumniDirectoryUI, ConsoleFormatter          │
├───────────────────────────────────────────────┤
│  Service Layer (com.alumconnect.service)       │
│  UserService, StudentProfileService,           │
│  AlumniProfileService, AlumniDirectoryService, │
│  MentorshipRequestService                      │
├───────────────────────────────────────────────┤
│  Repository Layer (com.alumconnect.repository) │
│  JdbcUserRepository, JdbcStudentProfileRepo,   │
│  JdbcAlumniProfileRepo, JdbcMentorshipReqRepo  │
├───────────────────────────────────────────────┤
│  Data / Infrastructure (com.alumconnect.util)  │
│  ConnectionPool, DatabaseInitializer,          │
│  PasswordUtil, ValidationUtil, UserSession     │
├───────────────────────────────────────────────┤
│  MySQL 8.0 (alumconnect_db)                    │
└───────────────────────────────────────────────┘
```

### Key Design Patterns

| Pattern               | Usage                                          |
|-----------------------|------------------------------------------------|
| **Singleton**         | `UserSession`, `ConnectionPool`                |
| **Repository**        | Interface + JDBC implementation per entity     |
| **DTO (Data Transfer Object)** | `LoginResult`, `RegistrationResult`, `MentorshipRequestResult` |
| **Service Layer**     | Business logic separated from UI               |
| **Builder**           | Used in model classes and filter criteria       |

### Test Strategy

- **540 total tests** across 33 test classes.
- **Unit tests** for all service, model, utility, and UI classes.
- **Integration tests** for JDBC repositories (skip gracefully without MySQL).
- **In-memory fakes** for repository testing without database.
- **Stream-based UI tests** using `ByteArrayInputStream` / `ByteArrayOutputStream`.

---

## Part G — Quick Demo Script (10-Minute Version)

For a shorter demo, follow this sequence:

1. **Build & Test:** `.\mvnw.cmd test` — show all 540 tests pass.
2. **Launch:** `.\mvnw.cmd compile exec:java`
3. **Register Student** → Register Alumni.
4. **Login as Student** → Create profile → Browse alumni → Send mentorship request.
5. **Logout** → **Login as Alumni** → Create profile → View requests → Accept request → View mentees.
6. **Logout** → **Login as Student** → View connections (verify accepted).
7. **Show invalid login** → friendly error.
8. **Exit application**.

---

## Part H — Troubleshooting

| Issue                           | Solution                                        |
|---------------------------------|-------------------------------------------------|
| `Connection refused` on startup | Ensure MySQL is running and credentials match    |
| `Access denied for user`        | Verify MySQL user/password in `database.properties` |
| Tests show 25 skipped           | Expected — DB integration tests need live MySQL  |
| `mvnw.cmd` not recognized       | Run from the project root directory              |
| Port conflict                   | Check MySQL is on port 3306 (default)            |

---

*Document prepared for AlumConnect v1.0.0 — Final College Submission*
