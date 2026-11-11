package com.alumconnect.web;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the AlumConnect web frontend layer:
 * HttpUtil, WebViews, WebSessionManager, and WebHandlers.
 */
class WebInterfaceTest {

    private UserService userService;
    private StudentProfileService studentProfileService;
    private AlumniProfileService alumniProfileService;
    private AlumniDirectoryService alumniDirectoryService;
    private MentorshipRequestService mentorshipRequestService;

    @BeforeEach
    void setUp() {
        WebSessionManager.clearAllSessions();
        UserSession.getInstance().logout();

        userService = new UserService();
        studentProfileService = new StudentProfileService();
        alumniProfileService = new AlumniProfileService();
        alumniDirectoryService = new AlumniDirectoryService();
        mentorshipRequestService = new MentorshipRequestService();
    }

    @AfterEach
    void tearDown() {
        WebSessionManager.clearAllSessions();
        UserSession.getInstance().logout();
    }

    // ── 1. HttpUtil Tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("HttpUtil: escapeHtml properly sanitizes dangerous characters")
    void testEscapeHtml() {
        assertEquals("", HttpUtil.escapeHtml(null));
        assertEquals("&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;",
                HttpUtil.escapeHtml("<script>alert(\"xss\")</script>"));
        assertEquals("A &amp; B &lt; C &#39;test&#39;",
                HttpUtil.escapeHtml("A & B < C 'test'"));
    }

    @Test
    @DisplayName("HttpUtil: parseQueryParams parses single and multiple parameters")
    void testParseQueryParams() {
        Map<String, String> empty = HttpUtil.parseQueryParams(URI.create("http://localhost/test"));
        assertTrue(empty.isEmpty());

        Map<String, String> params = HttpUtil.parseQueryParams(URI.create("http://localhost/test?role=STUDENT&msg=Hello+World"));
        assertEquals("STUDENT", params.get("role"));
        assertEquals("Hello World", params.get("msg"));
    }

    @Test
    @DisplayName("HttpUtil: parseFormData parses form URL-encoded payload")
    void testParseFormData() throws IOException {
        String body = "email=john%40example.com&role=STUDENT&notes=Hello+World";
        TestHttpExchange exchange = new TestHttpExchange("POST", "/test", body);

        Map<String, String> form = HttpUtil.parseFormData(exchange);
        assertEquals("john@example.com", form.get("email"));
        assertEquals("STUDENT", form.get("role"));
        assertEquals("Hello World", form.get("notes"));
    }

    @Test
    @DisplayName("HttpUtil: sendHtml sets 200, Content-Type and writes body")
    void testSendHtml() throws IOException {
        TestHttpExchange exchange = new TestHttpExchange("GET", "/test");
        HttpUtil.sendHtml(exchange, 200, "<h1>Hello</h1>");

        assertEquals(200, exchange.getResponseCode());
        assertEquals("text/html; charset=UTF-8", exchange.getResponseHeaders().getFirst("Content-Type"));
        assertEquals("<h1>Hello</h1>", exchange.getResponseBodyAsString());
    }

    @Test
    @DisplayName("HttpUtil: sendRedirect sets 302 and Location header")
    void testSendRedirect() throws IOException {
        TestHttpExchange exchange = new TestHttpExchange("GET", "/test");
        HttpUtil.sendRedirect(exchange, "/student/dashboard");

        assertEquals(302, exchange.getResponseCode());
        assertEquals("/student/dashboard", exchange.getResponseHeaders().getFirst("Location"));
    }

    @Test
    @DisplayName("HttpUtil: getCookie and setCookie manipulate headers properly")
    void testCookies() {
        TestHttpExchange exchange = new TestHttpExchange("GET", "/test");
        HttpUtil.setCookie(exchange, "MY_COOKIE", "token123", 3600);

        String setHeader = exchange.getResponseHeaders().getFirst("Set-Cookie");
        assertNotNull(setHeader);
        assertTrue(setHeader.contains("MY_COOKIE=token123"));

        exchange.getRequestHeaders().set("Cookie", "OTHER=abc; MY_COOKIE=token123; TEST=1");
        String readValue = HttpUtil.getCookie(exchange, "MY_COOKIE");
        assertEquals("token123", readValue);
    }

    // ── 2. WebViews Rendering Tests ──────────────────────────────────────────

    @Test
    @DisplayName("WebViews: renderHome generates valid HTML structure")
    void testRenderHome() {
        String guestHome = WebViews.renderHome(null);
        assertTrue(guestHome.contains("Welcome to AlumConnect"));
        assertTrue(guestHome.contains("/login"));
        assertTrue(guestHome.contains("/register"));

        Student student = new Student("alice@test.edu", "hash", "Alice Smith", "1234567890", "CSE", 2026);
        String userHome = WebViews.renderHome(student);
        assertTrue(userHome.contains("Go to My Dashboard"));
        assertTrue(userHome.contains("Alice Smith"));
    }

    @Test
    @DisplayName("WebViews: renderLogin renders form with error and success alerts")
    void testRenderLogin() {
        String loginHtml = WebViews.renderLogin("Invalid credentials", "Signed up successfully", "STUDENT", "alice@test.edu");
        assertTrue(loginHtml.contains("Invalid credentials"));
        assertTrue(loginHtml.contains("Signed up successfully"));
        assertTrue(loginHtml.contains("alice@test.edu"));
        assertTrue(loginHtml.contains("name=\"password\""));
    }

    @Test
    @DisplayName("WebViews: renderRegister renders registration form")
    void testRenderRegister() {
        String registerHtml = WebViews.renderRegister("Password too short", "ALUMNI", Map.of("fullName", "Bob Alum"));
        assertTrue(registerHtml.contains("Password too short"));
        assertTrue(registerHtml.contains("Bob Alum"));
        assertTrue(registerHtml.contains("Current Company"));
    }

    @Test
    @DisplayName("WebViews: renderStudentDashboard renders student details and directory")
    void testRenderStudentDashboard() {
        Student student = new Student("stu@test.edu", "hash", "Student Stu", "9999999999", "CSE", 2026);
        Alumni alum = new Alumni("alum@test.edu", "hash", "Alum Jane", "8888888888", "CSE", 2020);
        alum.setCurrentCompany("Google");
        alum.setCurrentRole("Staff Engineer");
        alum.setAvailableForMentorship(true);

        MentorshipRequest req = new MentorshipRequest(1, 2, "Career guidance", "Hello mentor");
        req.setId(101);
        req.setAlumniName("Alum Jane");
        req.setStatus(MentorshipStatus.PENDING);

        String dashHtml = WebViews.renderStudentDashboard(
                student, student, List.of(alum), null, List.of(req), Collections.emptyList(), "Request sent", null
        );

        assertTrue(dashHtml.contains("Welcome, Student Stu!"));
        assertTrue(dashHtml.contains("Google"));
        assertTrue(dashHtml.contains("Career guidance"));
        assertTrue(dashHtml.contains("Pending"));
    }

    @Test
    @DisplayName("WebViews: renderAlumniDashboard renders requests and actions")
    void testRenderAlumniDashboard() {
        Alumni alumni = new Alumni("alum@test.edu", "hash", "Alum Jane", "8888888888", "CSE", 2020);
        alumni.setCurrentCompany("Google");
        alumni.setCurrentRole("Staff Engineer");

        MentorshipRequest req = new MentorshipRequest(1, 2, "Interview Prep", "Need help with mock interview");
        req.setId(202);
        req.setStudentName("Student Stu");

        String dashHtml = WebViews.renderAlumniDashboard(
                alumni, alumni, List.of(req), Collections.emptyList(), "Action completed", null
        );

        assertTrue(dashHtml.contains("Welcome, Alum Jane!"));
        assertTrue(dashHtml.contains("Interview Prep"));
        assertTrue(dashHtml.contains("action=\"/alumni/request-action\""));
        assertTrue(dashHtml.contains("Accept"));
        assertTrue(dashHtml.contains("Reject"));
    }

    // ── 3. WebSessionManager Tests ───────────────────────────────────────────

    @Test
    @DisplayName("WebSessionManager: createSession, authenticate, and destroySession lifecycle")
    void testSessionLifecycle() {
        Student student = new Student("stu@test.edu", "hash", "Student Stu", "9999999999", "CSE", 2026);

        TestHttpExchange exchange = new TestHttpExchange("GET", "/login");
        String token = WebSessionManager.createSession(exchange, student);
        assertNotNull(token);

        // Put cookie on incoming request
        exchange.getRequestHeaders().set("Cookie", WebSessionManager.COOKIE_NAME + "=" + token);
        assertTrue(WebSessionManager.getAuthenticatedUser(exchange).isPresent());
        assertEquals("stu@test.edu", WebSessionManager.getAuthenticatedUser(exchange).get().getEmail());

        // Destroy session
        WebSessionManager.destroySession(exchange);
        assertTrue(WebSessionManager.getAuthenticatedUser(exchange).isEmpty());
    }

    @Test
    @DisplayName("WebSessionManager: executeAs bridges UserSession safely")
    void testSessionBridge() throws Exception {
        Student student = new Student("stu@test.edu", "hash", "Student Stu", "9999999999", "CSE", 2026);

        assertFalse(UserSession.getInstance().isLoggedIn());

        String result = WebSessionManager.executeAs(student, () -> {
            assertTrue(UserSession.getInstance().isLoggedIn());
            assertEquals("stu@test.edu", UserSession.getInstance().getCurrentUser().get().getEmail());
            return "SUCCESS";
        });

        assertEquals("SUCCESS", result);
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    // ── 4. WebHandlers Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("HomeHandler: GET / returns 200 OK")
    void testHomeHandler() throws IOException {
        WebHandlers.HomeHandler handler = new WebHandlers.HomeHandler();
        TestHttpExchange exchange = new TestHttpExchange("GET", "/");
        handler.handle(exchange);

        assertEquals(200, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("Welcome to AlumConnect"));
    }

    @Test
    @DisplayName("HomeHandler: GET /unknown returns 404 Not Found")
    void testHomeHandler404() throws IOException {
        WebHandlers.HomeHandler handler = new WebHandlers.HomeHandler();
        TestHttpExchange exchange = new TestHttpExchange("GET", "/non-existent");
        handler.handle(exchange);

        assertEquals(404, exchange.getResponseCode());
    }

    @Test
    @DisplayName("LoginHandler: GET /login returns 200 OK with login page")
    void testLoginHandlerGet() throws IOException {
        WebHandlers.LoginHandler handler = new WebHandlers.LoginHandler(userService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/login");
        handler.handle(exchange);

        assertEquals(200, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("Sign In to AlumConnect"));
    }

    @Test
    @DisplayName("RegisterHandler: GET /register returns 200 OK with register page")
    void testRegisterHandlerGet() throws IOException {
        WebHandlers.RegisterHandler handler = new WebHandlers.RegisterHandler(userService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/register");
        handler.handle(exchange);

        assertEquals(200, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("Create Your AlumConnect Account"));
    }

    @Test
    @DisplayName("LogoutHandler: GET /logout redirects to /login")
    void testLogoutHandler() throws IOException {
        WebHandlers.LogoutHandler handler = new WebHandlers.LogoutHandler(userService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/logout");
        handler.handle(exchange);

        assertEquals(302, exchange.getResponseCode());
        String loc = exchange.getResponseHeaders().getFirst("Location");
        assertNotNull(loc);
        assertTrue(loc.startsWith("/login"));
    }

    @Test
    @DisplayName("StudentDashboardHandler: Unauthenticated access redirects to /login")
    void testStudentDashboardUnauthenticated() throws IOException {
        WebHandlers.StudentDashboardHandler handler = new WebHandlers.StudentDashboardHandler(
                studentProfileService, alumniDirectoryService, mentorshipRequestService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/student/dashboard");
        handler.handle(exchange);

        assertEquals(302, exchange.getResponseCode());
        String loc = exchange.getResponseHeaders().getFirst("Location");
        assertNotNull(loc);
        assertTrue(loc.startsWith("/login"));
    }

    @Test
    @DisplayName("AlumniDashboardHandler: Unauthenticated access redirects to /login")
    void testAlumniDashboardUnauthenticated() throws IOException {
        WebHandlers.AlumniDashboardHandler handler = new WebHandlers.AlumniDashboardHandler(
                alumniProfileService, mentorshipRequestService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/alumni/dashboard");
        handler.handle(exchange);

        assertEquals(302, exchange.getResponseCode());
        String loc = exchange.getResponseHeaders().getFirst("Location");
        assertNotNull(loc);
        assertTrue(loc.startsWith("/login"));
    }

    @Test
    @DisplayName("RBAC: Alumni user accessing Student Dashboard gets 403 Forbidden")
    void testRbacAlumniAccessingStudentDashboard() throws IOException {
        Alumni alumni = new Alumni("alum@test.edu", "hash", "Alum Jane", "8888888888", "CSE", 2020);
        TestHttpExchange authExchange = new TestHttpExchange("GET", "/login");
        String token = WebSessionManager.createSession(authExchange, alumni);

        WebHandlers.StudentDashboardHandler handler = new WebHandlers.StudentDashboardHandler(
                studentProfileService, alumniDirectoryService, mentorshipRequestService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/student/dashboard");
        exchange.getRequestHeaders().set("Cookie", WebSessionManager.COOKIE_NAME + "=" + token);

        handler.handle(exchange);

        assertEquals(403, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("403 Forbidden"));
    }

    @Test
    @DisplayName("RBAC: Student user accessing Alumni Dashboard gets 403 Forbidden")
    void testRbacStudentAccessingAlumniDashboard() throws IOException {
        Student student = new Student("stu@test.edu", "hash", "Student Stu", "9999999999", "CSE", 2026);
        TestHttpExchange authExchange = new TestHttpExchange("GET", "/login");
        String token = WebSessionManager.createSession(authExchange, student);

        WebHandlers.AlumniDashboardHandler handler = new WebHandlers.AlumniDashboardHandler(
                alumniProfileService, mentorshipRequestService);
        TestHttpExchange exchange = new TestHttpExchange("GET", "/alumni/dashboard");
        exchange.getRequestHeaders().set("Cookie", WebSessionManager.COOKIE_NAME + "=" + token);

        handler.handle(exchange);

        assertEquals(403, exchange.getResponseCode());
        assertTrue(exchange.getResponseBodyAsString().contains("403 Forbidden"));
    }

    // ── 5. OpenBrowser Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("openBrowser: handles null and empty URLs safely without throwing exceptions")
    void testOpenBrowserEdgeCases() {
        assertFalse(AlumConnectWebServer.openBrowser(null));
        assertFalse(AlumConnectWebServer.openBrowser(""));
        assertFalse(AlumConnectWebServer.openBrowser("   "));
    }
}
