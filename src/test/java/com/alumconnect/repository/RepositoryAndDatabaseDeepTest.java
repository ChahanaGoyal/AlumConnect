package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep repository and database layer tests for Prompt 23E.
 *
 * <p>Uses standard Java dynamic proxies to inspect JDBC operations without requiring
 * external mocking frameworks or an active MySQL server.
 *
 * <p>Verifies:
 * <ul>
 *   <li>PreparedStatement parameterized query usage across all repositories</li>
 *   <li>SQL injection payload resistance (malicious strings safely bound)</li>
 *   <li>Try-with-resources resource closure (Connection, PreparedStatement, ResultSet)</li>
 *   <li>Transaction boundaries (setAutoCommit(false), commit(), rollback()) on multi-table operations</li>
 *   <li>Proper transaction rollback when second table insert fails in saveStudent/saveAlumni</li>
 *   <li>CRUD SQL operation integrity (INSERT, UPDATE, SELECT, DELETE)</li>
 *   <li>Ownership constraints in SQL WHERE clauses (user_id = ?, alumni_id = ?, student_id = ?)</li>
 *   <li>Safe SQL exception handling and credential leak prevention in database error formatters</li>
 * </ul>
 */
@DisplayName("Repository & Database Deep Tests (Prompt 23E)")
class RepositoryAndDatabaseDeepTest {

    // =========================================================================
    // 1. Parameterized Queries & SQL Injection Resistance
    // =========================================================================

    @Nested
    @DisplayName("PreparedStatement & Parameterized Query Verification")
    class PreparedStatementVerificationTests {

        @Test
        @DisplayName("existsByEmail uses parameterized PreparedStatement and binds email safely")
        void testExistsByEmailParameterized() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            String testEmail = "student@thapar.edu";
            repo.existsByEmail(testEmail);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("LOWER(email) = LOWER(?)"), "SQL must use '?' parameter placeholder: " + sql);
            assertEquals(testEmail, boundParams.get(1), "Parameter 1 must be bound to testEmail");
        }

        @Test
        @DisplayName("SQL injection payload in existsByEmail is safely bound as parameter literal")
        void testSqlInjectionInExistsByEmail() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            String injectionPayload = "' OR '1'='1' -- ";
            repo.existsByEmail(injectionPayload);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertFalse(sql.contains("OR '1'='1'"), "SQL query string must not contain injection payload");
            assertTrue(sql.contains("?"), "SQL must use '?' parameter placeholder");
            assertEquals(injectionPayload.trim(), boundParams.get(1));
        }

        @Test
        @DisplayName("findByEmail uses parameterized query and binds email")
        void testFindByEmailParameterized() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            String injectionPayload = "admin' OR 1=1; DROP TABLE users; --";
            repo.findByEmail(injectionPayload);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("WHERE LOWER(email) = LOWER(?)"));
            assertFalse(sql.contains("DROP TABLE"));
            assertEquals(injectionPayload.trim(), boundParams.get(1));
        }

        @Test
        @DisplayName("deleteUserById uses parameterized DELETE query")
        void testDeleteUserByIdParameterized() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            repo.deleteUserById(42);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("DELETE FROM users WHERE id = ?"), "SQL must be parameterized: " + sql);
            assertEquals(42, boundParams.get(1));
        }

        @Test
        @DisplayName("StudentProfile updateProfile uses parameterized UPDATE with user_id in WHERE clause")
        void testStudentProfileUpdateParameterized() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcStudentProfileRepository repo = new JdbcStudentProfileRepository(() -> proxyConn);

            Student student = new Student();
            student.setId(10);
            student.setDepartment("Computer Science");
            student.setGraduationYear(2026);
            student.setCurrentSemester(6);
            student.setEnrollmentNumber("102203001");

            repo.updateProfile(student);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("UPDATE student_profiles SET"), "Must be UPDATE query");
            assertTrue(sql.contains("WHERE user_id = ?"), "WHERE clause must be parameterized on user_id");
            assertEquals(10, boundParams.get(11), "Last parameter must be user_id (10)");
        }

        @Test
        @DisplayName("MentorshipRequest createRequest uses parameterized INSERT query with requested_at")
        void testMentorshipRequestCreateParameterized() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcMentorshipRequestRepository repo = new JdbcMentorshipRequestRepository(() -> proxyConn);

            MentorshipRequestDto dto = new MentorshipRequestDto(1, 2, "Topic", "Message");
            repo.createRequest(dto);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("INSERT INTO mentorship_requests"), "Must be INSERT query");
            // requested_at is now explicitly inserted as a parameterized placeholder (6th param)
            assertTrue(sql.contains("VALUES (?, ?, ?, ?, 'PENDING', ?)"), "Must use parameterized placeholders including requested_at");
            assertEquals(1, boundParams.get(1));
            assertEquals(2, boundParams.get(2));
            assertEquals("Topic", boundParams.get(3));
            assertEquals("Message", boundParams.get(4));
            // boundParams.get(5) is a LocalDateTime for requested_at — verify it is not null
            assertNotNull(boundParams.get(5), "requested_at parameter (index 5) must be set");
        }
    }

    // =========================================================================
    // 2. Transaction Management & Rollback Integrity
    // =========================================================================

    @Nested
    @DisplayName("Transaction Boundaries & Rollback Verification")
    class TransactionVerificationTests {

        @Test
        @DisplayName("saveStudent disables auto-commit, commits on success, and restores auto-commit")
        void testSaveStudentTransactionSuccess() throws SQLException {
            AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
            AtomicBoolean committed = new AtomicBoolean(false);
            AtomicBoolean autoCommitRestored = new AtomicBoolean(false);

            Connection proxyConn = createTransactionConnection(autoCommitDisabled, committed, new AtomicBoolean(false), autoCommitRestored, false);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            Student student = new Student("stud@thapar.edu", "hash", "Student", "+919876543210", "CS", 2026);
            student.setCurrentSemester(6);

            repo.saveStudent(student);

            assertTrue(autoCommitDisabled.get(), "setAutoCommit(false) must be called");
            assertTrue(committed.get(), "conn.commit() must be called on success");
            assertTrue(autoCommitRestored.get(), "setAutoCommit(true) must be restored in finally block");
            assertEquals(1, student.getId());
            assertEquals(1, student.getProfileId());
        }

        @Test
        @DisplayName("saveStudent rolls back transaction if profile insert fails")
        void testSaveStudentRollsBackOnProfileFailure() {
            AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
            AtomicBoolean committed = new AtomicBoolean(false);
            AtomicBoolean rolledBack = new AtomicBoolean(false);
            AtomicBoolean autoCommitRestored = new AtomicBoolean(false);

            // Fail on 2nd statement (profile insert)
            Connection proxyConn = createTransactionConnection(autoCommitDisabled, committed, rolledBack, autoCommitRestored, true);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            Student student = new Student("fail@thapar.edu", "hash", "Fail Student", "+919876543210", "CS", 2026);
            student.setCurrentSemester(6);

            SQLException thrown = assertThrows(SQLException.class, () -> repo.saveStudent(student));
            assertTrue(thrown.getMessage().contains("Simulated profile insert failure"));
            assertTrue(autoCommitDisabled.get(), "setAutoCommit(false) must have been called");
            assertFalse(committed.get(), "conn.commit() must NOT be called on failure");
            assertTrue(rolledBack.get(), "conn.rollback() must be called when profile insert fails");
            assertTrue(autoCommitRestored.get(), "setAutoCommit(true) must be restored");
        }

        @Test
        @DisplayName("saveAlumni disables auto-commit, commits on success, and restores auto-commit")
        void testSaveAlumniTransactionSuccess() throws SQLException {
            AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
            AtomicBoolean committed = new AtomicBoolean(false);
            AtomicBoolean autoCommitRestored = new AtomicBoolean(false);

            Connection proxyConn = createTransactionConnection(autoCommitDisabled, committed, new AtomicBoolean(false), autoCommitRestored, false);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            Alumni alumni = new Alumni("alum@thapar.edu", "hash", "Alumni", "+919811001122", "ECE", 2020);
            alumni.setYearsOfExperience(4);

            repo.saveAlumni(alumni);

            assertTrue(autoCommitDisabled.get(), "setAutoCommit(false) must be called");
            assertTrue(committed.get(), "conn.commit() must be called on success");
            assertTrue(autoCommitRestored.get(), "setAutoCommit(true) must be restored");
            assertEquals(1, alumni.getId());
            assertEquals(1, alumni.getProfileId());
        }

        @Test
        @DisplayName("saveAlumni rolls back transaction if profile insert fails")
        void testSaveAlumniRollsBackOnProfileFailure() {
            AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
            AtomicBoolean committed = new AtomicBoolean(false);
            AtomicBoolean rolledBack = new AtomicBoolean(false);
            AtomicBoolean autoCommitRestored = new AtomicBoolean(false);

            // Fail on 2nd statement (profile insert)
            Connection proxyConn = createTransactionConnection(autoCommitDisabled, committed, rolledBack, autoCommitRestored, true);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            Alumni alumni = new Alumni("fail.alum@thapar.edu", "hash", "Fail Alumni", "+919811001122", "ECE", 2020);

            SQLException thrown = assertThrows(SQLException.class, () -> repo.saveAlumni(alumni));
            assertTrue(thrown.getMessage().contains("Simulated profile insert failure"));
            assertTrue(autoCommitDisabled.get(), "setAutoCommit(false) must have been called");
            assertFalse(committed.get(), "conn.commit() must NOT be called");
            assertTrue(rolledBack.get(), "conn.rollback() must be called when alumni profile insert fails");
            assertTrue(autoCommitRestored.get(), "setAutoCommit(true) must be restored");
        }
    }

    // =========================================================================
    // 3. Resource Management & Closure Checks
    // =========================================================================

    @Nested
    @DisplayName("Try-with-Resources & Resource Closure Verification")
    class ResourceClosureTests {

        @Test
        @DisplayName("Connection and PreparedStatement are closed after existsByEmail")
        void testConnectionClosedAfterExistsByEmail() throws SQLException {
            AtomicBoolean connClosed = new AtomicBoolean(false);
            AtomicBoolean psClosed = new AtomicBoolean(false);

            Connection proxyConn = createClosureTrackingConnection(connClosed, psClosed);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            repo.existsByEmail("test@thapar.edu");

            assertTrue(psClosed.get(), "PreparedStatement must be closed");
            assertTrue(connClosed.get(), "Connection must be closed / returned to pool");
        }

        @Test
        @DisplayName("Connection and PreparedStatement are closed after deleteUserById")
        void testConnectionClosedAfterDelete() throws SQLException {
            AtomicBoolean connClosed = new AtomicBoolean(false);
            AtomicBoolean psClosed = new AtomicBoolean(false);

            Connection proxyConn = createClosureTrackingConnection(connClosed, psClosed);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            repo.deleteUserById(99);

            assertTrue(psClosed.get(), "PreparedStatement must be closed");
            assertTrue(connClosed.get(), "Connection must be closed");
        }

        @Test
        @DisplayName("Resources are properly closed even when query throws SQLException")
        void testResourcesClosedOnQueryFailure() {
            AtomicBoolean connClosed = new AtomicBoolean(false);
            AtomicBoolean psClosed = new AtomicBoolean(false);

            Connection proxyConn = createFailingQueryConnection(connClosed, psClosed);
            JdbcUserRepository repo = new JdbcUserRepository(() -> proxyConn);

            assertThrows(SQLException.class, () -> repo.existsByEmail("error@thapar.edu"));

            assertTrue(psClosed.get(), "PreparedStatement must be closed even on exception");
            assertTrue(connClosed.get(), "Connection must be closed even on exception");
        }
    }

    // =========================================================================
    // 4. Ownership Constraints in SQL WHERE Clauses
    // =========================================================================

    @Nested
    @DisplayName("Ownership Constraints & Isolation in Repository Queries")
    class OwnershipConstraintsTests {

        @Test
        @DisplayName("Student profile lookup query enforces user_id AND role = 'STUDENT'")
        void testStudentProfileLookupSQL() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcStudentProfileRepository repo = new JdbcStudentProfileRepository(() -> proxyConn);

            repo.findProfileByUserId(5);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("WHERE u.id = ? AND u.role = 'STUDENT'"),
                    "Query must lock by user_id AND student role: " + sql);
            assertEquals(5, boundParams.get(1));
        }

        @Test
        @DisplayName("Alumni profile lookup query enforces user_id AND role = 'ALUMNI'")
        void testAlumniProfileLookupSQL() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcAlumniProfileRepository repo = new JdbcAlumniProfileRepository(() -> proxyConn);

            repo.findProfileByUserId(7);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("WHERE u.id = ? AND u.role = 'ALUMNI'"),
                    "Query must lock by user_id AND alumni role: " + sql);
            assertEquals(7, boundParams.get(1));
        }

        @Test
        @DisplayName("Mentorship requests for alumni query enforces alumni_id in WHERE clause")
        void testMentorshipRequestsByAlumniSQL() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcMentorshipRequestRepository repo = new JdbcMentorshipRequestRepository(() -> proxyConn);

            repo.findRequestsByAlumniId(8);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("WHERE mr.alumni_id = ?"), "Must lock by alumni_id: " + sql);
            assertEquals(8, boundParams.get(1));
        }

        @Test
        @DisplayName("Mentorship requests for student query enforces student_id in WHERE clause")
        void testMentorshipRequestsByStudentSQL() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcMentorshipRequestRepository repo = new JdbcMentorshipRequestRepository(() -> proxyConn);

            repo.findRequestsByStudentId(3);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("WHERE mr.student_id = ?"), "Must lock by student_id: " + sql);
            assertEquals(3, boundParams.get(1));
        }

        @Test
        @DisplayName("Accepted connections query filters by status = 'ACCEPTED'")
        void testAcceptedConnectionsSQLFiltersByStatus() throws SQLException {
            List<String> preparedSqls = new ArrayList<>();
            Map<Integer, Object> boundParams = new HashMap<>();

            Connection proxyConn = createTrackingConnection(preparedSqls, boundParams);
            JdbcMentorshipRequestRepository repo = new JdbcMentorshipRequestRepository(() -> proxyConn);

            repo.findAcceptedConnectionsByStudentId(15);

            assertEquals(1, preparedSqls.size());
            String sql = preparedSqls.get(0);
            assertTrue(sql.contains("status = 'ACCEPTED'"), "Connections query must restrict status: " + sql);
            assertTrue(sql.contains("mr.student_id = ?"), "Must lock by student_id: " + sql);
            assertEquals(15, boundParams.get(1));
        }
    }

    // =========================================================================
    // 5. Database Failures & Credential Masking in Service-Repository Interop
    // =========================================================================

    @Nested
    @DisplayName("Database Failures & Credential Masking")
    class DatabaseFailureAndCredentialMaskingTests {

        @Test
        @DisplayName("Database link failure in UserService registration returns masked user-friendly error")
        void testDatabaseLinkFailureInRegistration() {
            // Supplier throws link failure
            JdbcUserRepository failingRepo = new JdbcUserRepository(() -> {
                throw new SQLException("Communications link failure to jdbc:mysql://db.production.internal:3306/alumconnect", "08S01");
            });

            UserService svc = new UserService(failingRepo);
            RegistrationRequest req = RegistrationRequest.forStudent("Test", "test.db@thapar.edu", "ValidP@ss1", "CS", 2026);
            req.setConfirmPassword("ValidP@ss1");

            RegistrationResult result = svc.register(req);

            assertFalse(result.isSuccess());
            assertFalse(result.getMessage().contains("db.production.internal"), "Internal host must not be exposed");
            assertFalse(result.getMessage().contains("3306"), "Internal port must not be exposed");
            assertTrue(result.getMessage().contains("Database is currently unavailable. Please try again later."));
        }

        @Test
        @DisplayName("Database error message sanitization completely masks raw passwords")
        void testDatabaseErrorSanitizationMasksPassword() {
            SQLException ex = new SQLException("Access denied for user 'admin'@'localhost' (using password=SuperSecretPassword123)");
            String formatted = ValidationUtil.formatDatabaseErrorMessage(ex, "Registration failed");

            assertFalse(formatted.contains("SuperSecretPassword123"), "Raw password must be stripped");
            assertTrue(formatted.contains("******") || formatted.contains("unavailable"));
        }
    }

    // =========================================================================
    // Dynamic Proxy Helpers for JDBC Inspection (No External Mocking Libs)
    // =========================================================================

    private static Connection createTrackingConnection(List<String> preparedSqls, Map<Integer, Object> boundParams) {
        InvocationHandler connHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("prepareStatement".equals(name)) {
                String sql = (String) args[0];
                preparedSqls.add(sql);
                return createTrackingPreparedStatement(boundParams);
            }
            if ("close".equals(name) || "isClosed".equals(name) || "isValid".equals(name)) {
                return false;
            }
            if ("getAutoCommit".equals(name)) return true;
            return null;
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connHandler
        );
    }

    private static PreparedStatement createTrackingPreparedStatement(Map<Integer, Object> boundParams) {
        InvocationHandler psHandler = (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                boundParams.put((Integer) args[0], args[1]);
                return null;
            }
            if ("executeQuery".equals(name)) {
                return createEmptyResultSet();
            }
            if ("executeUpdate".equals(name)) {
                return 1;
            }
            if ("getGeneratedKeys".equals(name)) {
                return createSingleKeyResultSet(1);
            }
            if ("close".equals(name)) {
                return null;
            }
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                psHandler
        );
    }

    private static Connection createTransactionConnection(AtomicBoolean autoCommitDisabled,
                                                          AtomicBoolean committed,
                                                          AtomicBoolean rolledBack,
                                                          AtomicBoolean autoCommitRestored,
                                                          boolean failOnSecondStatement) {
        AtomicInteger statementCounter = new AtomicInteger(0);

        InvocationHandler connHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("setAutoCommit".equals(name)) {
                boolean val = (Boolean) args[0];
                if (!val) {
                    autoCommitDisabled.set(true);
                } else {
                    autoCommitRestored.set(true);
                }
                return null;
            }
            if ("getAutoCommit".equals(name)) return true;
            if ("commit".equals(name)) {
                committed.set(true);
                return null;
            }
            if ("rollback".equals(name)) {
                rolledBack.set(true);
                return null;
            }
            if ("prepareStatement".equals(name)) {
                int count = statementCounter.incrementAndGet();
                if (failOnSecondStatement && count >= 2) {
                    return createFailingPreparedStatement("Simulated profile insert failure");
                }
                return createSuccessPreparedStatement();
            }
            if ("close".equals(name)) return null;
            return null;
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connHandler
        );
    }

    private static Connection createClosureTrackingConnection(AtomicBoolean connClosed, AtomicBoolean psClosed) {
        InvocationHandler connHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("close".equals(name)) {
                connClosed.set(true);
                return null;
            }
            if ("prepareStatement".equals(name)) {
                return createClosureTrackingPreparedStatement(psClosed);
            }
            if ("getAutoCommit".equals(name)) return true;
            return null;
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connHandler
        );
    }

    private static PreparedStatement createClosureTrackingPreparedStatement(AtomicBoolean psClosed) {
        InvocationHandler psHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("close".equals(name)) {
                psClosed.set(true);
                return null;
            }
            if ("executeQuery".equals(name)) return createEmptyResultSet();
            if ("executeUpdate".equals(name)) return 1;
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                psHandler
        );
    }

    private static Connection createFailingQueryConnection(AtomicBoolean connClosed, AtomicBoolean psClosed) {
        InvocationHandler connHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("close".equals(name)) {
                connClosed.set(true);
                return null;
            }
            if ("prepareStatement".equals(name)) {
                return createFailingPreparedStatementOnExecute(psClosed);
            }
            if ("getAutoCommit".equals(name)) return true;
            return null;
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connHandler
        );
    }

    private static PreparedStatement createFailingPreparedStatement(String errorMsg) {
        InvocationHandler psHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("executeUpdate".equals(name) || "executeQuery".equals(name)) {
                throw new SQLException(errorMsg);
            }
            if ("close".equals(name)) return null;
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                psHandler
        );
    }

    private static PreparedStatement createFailingPreparedStatementOnExecute(AtomicBoolean psClosed) {
        InvocationHandler psHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("close".equals(name)) {
                psClosed.set(true);
                return null;
            }
            if ("executeQuery".equals(name) || "executeUpdate".equals(name)) {
                throw new SQLException("Simulated syntax or database error");
            }
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                psHandler
        );
    }

    private static PreparedStatement createSuccessPreparedStatement() {
        InvocationHandler psHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("executeUpdate".equals(name)) return 1;
            if ("executeQuery".equals(name)) return createEmptyResultSet();
            if ("getGeneratedKeys".equals(name)) return createSingleKeyResultSet(1);
            if ("close".equals(name)) return null;
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                psHandler
        );
    }

    private static ResultSet createEmptyResultSet() {
        InvocationHandler rsHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) return false;
            if ("close".equals(name)) return null;
            return null;
        };
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                rsHandler
        );
    }

    private static ResultSet createSingleKeyResultSet(int key) {
        AtomicBoolean read = new AtomicBoolean(false);
        InvocationHandler rsHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) {
                return !read.getAndSet(true);
            }
            if ("getInt".equals(name)) return key;
            if ("close".equals(name)) return null;
            return null;
        };
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                rsHandler
        );
    }
}
