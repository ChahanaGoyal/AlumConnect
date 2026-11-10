# AlumConnect — User Guide

> Comprehensive end-user manual for students and alumni using the **AlumConnect** console networking and mentorship platform.

---

## 1. Getting Started

### 1.1. Launching the Application
Launch AlumConnect in your terminal using the Maven Wrapper or Java executable:

```bash
# Using Maven Exec Plugin (recommended)
.\mvnw.cmd compile exec:java       # On Windows
./mvnw compile exec:java           # On Linux / macOS

# Alternatively, using the packaged JAR
java -jar target/alumconnect-1.0-SNAPSHOT.jar
```

Upon startup, you will see the primary **Main Menu**:

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

---

### 1.2. Account Registration
If you do not have an account, enter `2` at the Main Menu prompt:

1. **Select Account Role:**
   * Enter `1` for **Student**.
   * Enter `2` for **Alumni**.
2. **Enter Personal Details:**
   * **Full Name:** Enter your first and last name (e.g., `Aarav Patel`).
   * **Email Address:** Enter a valid, unique institutional or personal email (e.g., `aarav.student@alumconnect.test`).
   * **Password:** Must be at least 8 characters long and contain both letters and digits (e.g., `StudentP@ss123`).
   * **Phone Number:** Optional contact number (press Enter to skip).
3. **Enter Initial Academic/Professional Details:**
   * **For Students:** Degree (default: `Bachelor of Technology`), Department (e.g., `Computer Science`), and expected Graduation Year (e.g., `2026`).
   * **For Alumni:** Department, Graduation Year (e.g., `2019`), Current Company (e.g., `Amazon`), Job Role (e.g., `Senior SDE`), and Industry (e.g., `Technology`).
4. **Completion:** Upon successful registration, you will receive a confirmation banner and be returned to the Main Menu ready to log in.

---

### 1.3. Logging In
To access your account, enter `1` at the Main Menu prompt:

1. **Email:** Enter your registered email address.
   * *Tip:* Enter `0` or `cancel` at any time to abort login and return to the Main Menu.
2. **Password:** Enter your account password.
3. **Authentication:** The system verifies your credentials against salted hashes in the database.
4. **Dashboard Routing:** On success, you are automatically directed to your role-specific dashboard:
   * Enrolled students are routed to the **Student Dashboard**.
   * Graduated alumni are routed to the **Alumni Dashboard**.

---

### 1.4. Logging Out & Exiting
* **Logout:** Enter `9` on either the Student Dashboard or Alumni Dashboard. This invalidates your active session and safely returns you to the Main Menu.
* **Exit:** Enter `0`, `EXIT`, `Q`, or `QUIT` from the Main Menu to shut down the application and release connection pool resources.

---

## 2. Student Guide

When authenticated as a Student, the **Student Dashboard** is displayed:

```text
==================================================
                   ALUMCONNECT                    
            Alumni Networking Platform            
==================================================
                STUDENT DASHBOARD                 
--------------------------------------------------
Welcome, Aarav Patel!
Department: Computer Science | Semester: 6 | Grad: 2026
--------------------------------------------------
Available Options:
  [1] My Profile (View / Edit)
  [2] Browse Alumni Directory
  [3] Search Alumni Directory
  [4] Filter Alumni Directory
  [5] My Mentorship Requests
  [6] My Mentorship Connections
  [9] Logout
  [0] Exit
--------------------------------------------------
Enter your choice: 
```

---

### 2.1. Managing the Student Profile
Select `[1] My Profile` to open the profile sub-menu:

```text
--- My Profile ---
  [1] View My Profile
  [2] Update My Profile
  [0] Back to Student Dashboard
Enter your choice: 
```

* **View Profile (`[1]`):** Displays full academic details, enrolled semester, bio, skills, interests, LinkedIn URL, and GitHub profile URL.
* **Update Profile (`[2]`):** Prompts you to update your profile fields. Press Enter on any field to keep its existing value:
  * Current Semester (1–12)
  * Bio / Career Summary
  * Skills (e.g., `Java, Spring Boot, MySQL, Docker`)
  * Research / Career Interests
  * LinkedIn and GitHub URLs
* **Return (`[0]`):** Safely exits the profile sub-menu back to the Student Dashboard.

---

### 2.2. Browsing the Alumni Directory
Select `[2] Browse Alumni Directory`:
* Displays all verified alumni registered on the platform in clean 50-character profile cards:
  ```text
  --------------------------------------------------
  [ID: 102] Priya Sharma
  Degree: Bachelor of Technology | Department: Computer Science
  Class of: 2018 | Experience: 6 years
  Company: Microsoft | Role: Senior Product Manager
  Industry: Technology
  Mentorship: AVAILABLE
  --------------------------------------------------
  ```
* After viewing the list, you can enter any Alumni ID to inspect their detailed profile or enter `0` to return.

---

### 2.3. Searching and Filtering Alumni

#### Keyword Search (`[3]`)
* Select `[3] Search Alumni Directory`.
* Enter any search keyword (e.g., `Google`, `Data Scientist`, `FinTech`, or `Python`).
* The system performs keyword matching across alumni names, employers, roles, industries, and expertise keywords.

#### Structured Filtering (`[4]`)
* Select `[4] Filter Alumni Directory`.
* Apply one or more filters:
  * **Department:** e.g., `Computer Science`
  * **Industry:** e.g., `Technology` or `Finance`
  * **Minimum Years of Experience:** e.g., `5`
* Only alumni satisfying all specified filter criteria are displayed. If no alumni match, a friendly empty-state message is shown.

---

### 2.4. Viewing Detailed Alumni Profiles & Sending Mentorship Requests
When selecting an alumnus ID from directory, search, or filter results:

1. The detailed profile card displays the alumnus's bio, LinkedIn URL, and maximum mentee capacity.
2. If the alumnus is available for mentorship, you are prompted:
   ```text
   Actions:
     [1] Send Mentorship Request
     [0] Back
   ```
3. Enter `1` to initiate a request:
   * **Topic:** Enter a concise subject (e.g., `Resume Review for Backend Roles`).
   * **Message:** Explain your background and what guidance you are seeking.
4. **Validation:**
   * You cannot send a mentorship request to yourself.
   * You cannot send a duplicate pending request to the same mentor.
5. Once sent, the request status is initialized to `PENDING`.

---

### 2.5. Checking Mentorship Request Statuses
Select `[5] My Mentorship Requests`:
* Lists all requests you have submitted to alumni.
* Displays:
  * Targeted Alumnus Name and Company.
  * Submission Timestamp (`YYYY-MM-DD HH:mm:ss`).
  * Mentorship Topic and Message.
  * **Current Status:** `PENDING`, `ACCEPTED`, or `REJECTED`.
  * **Alumni Feedback Notes:** Feedback or meeting notes provided when the alumnus responded.

---

### 2.6. Viewing Accepted Mentorship Connections
Select `[6] My Mentorship Connections`:
* Displays all alumni who have accepted your mentorship requests.
* Shows verified contact information (Name, Email, Department, Company, Job Role, and LinkedIn link) so you can coordinate mentorship sessions.

---

## 3. Alumni Guide

When authenticated as an Alumnus, the **Alumni Dashboard** is displayed:

```text
==================================================
                   ALUMCONNECT                    
            Alumni Networking Platform            
==================================================
                 ALUMNI DASHBOARD                 
--------------------------------------------------
Welcome, Meera Nair!
Company: Amazon | Role: Principal SDE | Exp: 7 years
Mentorship: AVAILABLE (Max Mentees: 4)
--------------------------------------------------
Available Options:
  [1] My Profile (View / Edit)
  [2] View Incoming Mentorship Requests
  [3] My Mentees (Accepted Connections)
  [9] Logout
  [0] Exit
--------------------------------------------------
Enter your choice: 
```

---

### 3.1. Managing the Alumni Profile & Mentorship Capacity
Select `[1] My Profile`:

* **View Profile (`[1]`):** Displays current professional information, employer, job title, industry, years of experience, expertise areas, bio, and LinkedIn URL.
* **Update Profile (`[2]`):** Edit company, role, industry, years of experience, expertise keywords, and biography.
* **Mentorship Availability Toggle:**
  * Set `Available for Mentorship` to `true` or `false`.
  * Setting availability to `false` automatically hides your profile from mentorship request actions while keeping your directory card active.
* **Max Mentees Capacity:**
  * Configure the maximum number of concurrent mentees you can mentor (default: 3).
  * If your accepted mentees equal this number, the platform automatically pauses incoming requests until bandwidth opens up.
* **Return (`[0]`):** Returns to the Alumni Dashboard.

---

### 3.2. Viewing Incoming Mentorship Requests
Select `[2] View Incoming Mentorship Requests`:
* Displays all pending mentorship requests submitted by students to your account:
  ```text
  --------------------------------------------------
  [Request ID: 42] From Student: Aarav Patel
  Degree: B.Tech | Department: Computer Science | Class of: 2026
  Topic: Resume Review for Backend Roles
  Requested At: 2026-11-10 14:30:00
  Message: Hello! I am preparing for SDE interviews and would value your feedback on my distributed systems projects.
  --------------------------------------------------
  ```
* If no pending requests exist, the dashboard displays:
  `[INFO] You have no pending mentorship requests at this time.`

---

### 3.3. Accepting or Rejecting Mentorship Requests
From the incoming request queue:

1. Enter the target **Request ID** to take action.
2. Select an option:
   * Enter `A` or `1` to **Accept**.
   * Enter `R` or `2` to **Reject**.
   * Enter `0` to **Cancel** and return to the dashboard.
3. **Provide Response Notes:**
   * When **Accepting:** Provide guidance on next steps (e.g., `Glad to help! Please email me at meera@amazon.com with your resume draft.`).
   * When **Rejecting:** Provide constructive feedback (e.g., `Apologies, currently at full capacity for Q4 mentorship. Best wishes!`).
4. **Outcome:**
   * If accepted, the student is immediately added to your active connections roster (`My Mentees`).
   * If rejected, the request is closed; the student sees your note, and no connection is formed.

---

### 3.4. Viewing Mentorship Connections (My Mentees)
Select `[3] My Mentees`:
* Lists all students whose mentorship requests you have accepted.
* Displays student full names, contact email addresses, departments, enrolled semesters, graduation years, and the original mentorship topic.
