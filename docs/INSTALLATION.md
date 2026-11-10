# AlumConnect — Installation & Deployment Guide

> Step-by-step setup, configuration, database provisioning, testing, and execution instructions for the **AlumConnect** platform.

---

## 1. System Requirements & Prerequisites

Before setting up AlumConnect, ensure your development environment satisfies the following prerequisites:

| Component | Minimum Version | Verified Tested Version | Notes |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | OpenJDK / Oracle JDK 17 (LTS) | JDK 17 & Oracle JDK 26 | Must be on system `PATH` with `JAVA_HOME` configured. |
| **Build Tool (Maven)** | Apache Maven 3.8+ (Optional) | Maven Wrapper 3.9.9 (Included) | Repository includes `mvnw` (Unix) and `mvnw.cmd` (Windows). |
| **Relational Database** | MySQL Server 8.0+ | MySQL 8.0 / 8.4 Community Edition | Running on port `3306` with InnoDB storage engine. |
| **Operating System** | Cross-platform | Windows 10/11, macOS, Linux | Uses standard ANSI/ASCII console formatting. |

---

## 2. Verifying Java & Maven Environment

Open a terminal or command prompt and verify your Java installation:

```bash
# Verify Java installation (requires JDK 17+)
java -version

# Verify JAVA_HOME is configured
# On Windows PowerShell:
$env:JAVA_HOME

# On Linux / macOS:
echo $JAVA_HOME
```

*Expected output:*
```text
java version "17.0.x" (or higher)
Java(TM) SE Runtime Environment
```

---

## 3. MySQL Server Installation & Setup

### 3.1. Install MySQL Server
If MySQL is not installed:
* **Windows:** Download and install the [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) using the MySQL Installer.
* **macOS:** Install via Homebrew: `brew install mysql`
* **Linux (Ubuntu/Debian):** Install via APT: `sudo apt update && sudo apt install mysql-server`

### 3.2. Start the MySQL Service
Ensure the MySQL service is running on `localhost:3306`:

```bash
# Windows (Command Prompt as Administrator)
net start MySQL80

# macOS
brew services start mysql

# Linux (systemd)
sudo systemctl start mysql
sudo systemctl status mysql
```

---

## 4. Database Creation & Schema Provisioning

### 4.1. Automatic Initialization (Default)
AlumConnect includes an automated database bootstrapper ([`DatabaseInitializer.java`](file:///c:/coding/AlumConnect/AlumConnect/src/main/java/com/alumconnect/util/DatabaseInitializer.java)). When the application starts, it connects to MySQL, executes [`schema.sql`](file:///c:/coding/AlumConnect/AlumConnect/schema.sql), and creates:
* Database: `alumconnect_db` (character set `utf8mb4`)
* Tables: `users`, `student_profiles`, `alumni_profiles`, `mentorship_requests`
* Foreign keys, cascades, indexes, and format check constraints.

### 4.2. Manual Initialization (Optional)
If you prefer provisioning the database manually prior to launch:

1. Connect to MySQL via terminal or MySQL Workbench:
   ```bash
   mysql -u your_username -p
   ```
2. Execute the provided DDL script:
   ```sql
   SOURCE schema.sql;
   ```
3. Verify that all 4 tables are present:
   ```sql
   USE alumconnect_db;
   SHOW TABLES;
   ```
   *Output:*
   ```text
   +---------------------------+
   | Tables_in_alumconnect_db  |
   +---------------------------+
   | alumni_profiles           |
   | mentorship_requests       |
   | student_profiles          |
   | users                     |
   +---------------------------+
   ```

---

## 5. Database Configuration

Database configuration parameters are loaded from [`src/main/resources/application.properties`](file:///c:/coding/AlumConnect/AlumConnect/src/main/resources/application.properties):

```properties
# Primary database URL (connection pool targets this database)
db.url=jdbc:mysql://localhost:3306/alumconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

# Server root URL (used by DatabaseInitializer during bootstrapping)
db.server.url=jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

# Credentials (replace placeholders with your MySQL credentials)
db.user=your_username
db.password=your_password
db.driver=com.mysql.cj.jdbc.Driver

# Connection Pool Settings
db.pool.maxSize=10
db.pool.minIdle=2
db.pool.connectionTimeoutMs=3000
db.pool.validationQuery=SELECT 1

# Application Identity
app.name=AlumConnect
app.version=1.0.0
```

### Overriding Credentials without Modifying Files
You can configure credentials securely using environment variables or JVM system properties:

#### Option A: Using Environment Variables
```bash
# Windows PowerShell
$env:DB_USER="your_username"
$env:DB_PASSWORD="your_password"
$env:DB_URL="jdbc:mysql://localhost:3306/alumconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

# Linux / macOS
export DB_USER="your_username"
export DB_PASSWORD="your_password"
export DB_URL="jdbc:mysql://localhost:3306/alumconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

#### Option B: Using JVM System Properties
Pass flags directly during execution:
`-Ddb.user=your_username -Ddb.password=your_password`

---

## 6. Running Tests & Regression Suite

AlumConnect features a comprehensive JUnit 5 test suite consisting of 540 tests.

### Execute All Tests
```bash
# On Windows
.\mvnw.cmd clean test

# On Linux / macOS
./mvnw clean test
```

### Execute a Specific Test Suite
```bash
# Windows
.\mvnw.cmd test -Dtest=AuthenticationWorkflowTest
.\mvnw.cmd test -Dtest=StudentWorkflowTest
.\mvnw.cmd test -Dtest=AlumniAndMentorshipWorkflowTest
.\mvnw.cmd test -Dtest=AccessControlAndValidationDeepTest
.\mvnw.cmd test -Dtest=RepositoryAndDatabaseDeepTest
.\mvnw.cmd test -Dtest=UIPolishAndExperienceTest

# Linux / macOS
./mvnw test -Dtest=AuthenticationWorkflowTest
```

> [!NOTE]
> **Database Integration Tests Behavior:**
> When MySQL is not running on `localhost:3306`, the 25 live-database integration tests gracefully skip using JUnit 5 `Assumptions.assumeTrue(...)`. All other 515 unit, mock, reflection, and proxy tests execute and pass with `BUILD SUCCESS`.

---

## 7. Starting the Application

You can start the AlumConnect interactive console application using either of the two methods below:

### Method 1: Using Maven Exec Plugin (Recommended)
This method automatically resolves all dependencies on the classpath:

```bash
# On Windows
.\mvnw.cmd compile exec:java

# On Linux / macOS
./mvnw compile exec:java
```

### Method 2: Building and Running the Executable JAR
Package the project into a standalone JAR file:

```bash
# Build package
# Windows:
.\mvnw.cmd clean package -DskipTests

# Linux / macOS:
./mvnw clean package -DskipTests

# Run the packaged JAR
java -jar target/alumconnect-1.0-SNAPSHOT.jar
```

---

## 8. Troubleshooting & FAQ

### Issue: "Access denied for user 'root'@'localhost'"
* **Root Cause:** The password in `src/main/resources/application.properties` does not match your MySQL server password.
* **Resolution:** Update `db.password=your_actual_password` or export the `$env:DB_PASSWORD` environment variable.

### Issue: "Could not obtain a database connection: Connection refused"
* **Root Cause:** MySQL Server is stopped or running on a different port.
* **Resolution:** Verify service status (`net start MySQL80` or `sudo systemctl status mysql`) and confirm it listens on port 3306.

### Issue: "java: release version 17 not supported"
* **Root Cause:** The active JDK on your `PATH` is older than Java 17.
* **Resolution:** Install JDK 17 (or newer) and ensure `JAVA_HOME` points to the new installation.
