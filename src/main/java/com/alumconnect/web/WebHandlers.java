package com.alumconnect.web;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.LoginResult;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.service.dto.StudentProfileResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP request handlers for the AlumConnect web application.
 * Connects web routes to existing backend business services.
 */
public final class WebHandlers {

    private static final Logger LOGGER = Logger.getLogger(WebHandlers.class.getName());

    private WebHandlers() {
        // Prevent instantiation
    }

    // ── 1. Home Handler ──────────────────────────────────────────────────────

    public static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"/".equals(exchange.getRequestURI().getPath())) {
                HttpUtil.sendHtml(exchange, 404, "<h1>404 Not Found</h1><p>Page not found.</p><a href='/'>Go to Home</a>");
                return;
            }
            Optional<User> currentUser = WebSessionManager.getAuthenticatedUser(exchange);
            String html = WebViews.renderHome(currentUser.orElse(null));
            HttpUtil.sendHtml(exchange, 200, html);
        }
    }

    // ── 2. Login Handler ─────────────────────────────────────────────────────

    public static class LoginHandler implements HttpHandler {
        private final UserService userService;

        public LoginHandler(UserService userService) {
            this.userService = userService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Optional<User> currentUser = WebSessionManager.getAuthenticatedUser(exchange);
                if (currentUser.isPresent()) {
                    User u = currentUser.get();
                    String dest = u.getRole() == Role.STUDENT ? "/student/dashboard" : "/alumni/dashboard";
                    HttpUtil.sendRedirect(exchange, dest);
                    return;
                }

                Map<String, String> query = HttpUtil.parseQueryParams(exchange.getRequestURI());
                String error = query.get("error");
                String success = query.get("success");
                String role = query.getOrDefault("role", "STUDENT");
                String email = query.getOrDefault("email", "");

                String html = WebViews.renderLogin(error, success, role, email);
                HttpUtil.sendHtml(exchange, 200, html);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> form = HttpUtil.parseFormData(exchange);
                String email = form.getOrDefault("email", "").trim();
                String password = form.getOrDefault("password", "");
                String selectedRoleStr = form.getOrDefault("role", "STUDENT").trim();

                Role selectedRole;
                try {
                    selectedRole = Role.valueOf(selectedRoleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    selectedRole = Role.STUDENT;
                }

                LoginResult result = userService.login(email, password);
                if (!result.isSuccess() || result.getUser() == null) {
                    String html = WebViews.renderLogin(result.getMessage(), null, selectedRole.name(), email);
                    HttpUtil.sendHtml(exchange, 200, html);
                    return;
                }

                User user = result.getUser();

                // Role matching guard: verify selected login role matches actual account role
                if (user.getRole() != selectedRole) {
                    userService.logout(); // Clear console session
                    String err = "Role mismatch: This account is registered as a " + user.getRole() +
                            ". Please select " + (user.getRole() == Role.STUDENT ? "Student" : "Alumni") + " login.";
                    String html = WebViews.renderLogin(err, null, selectedRole.name(), email);
                    HttpUtil.sendHtml(exchange, 200, html);
                    return;
                }

                // Create web session
                WebSessionManager.createSession(exchange, user);
                String redirectTarget = user.getRole() == Role.STUDENT ? "/student/dashboard" : "/alumni/dashboard";
                HttpUtil.sendRedirect(exchange, redirectTarget);
                return;
            }

            HttpUtil.sendText(exchange, 405, "Method Not Allowed");
        }
    }

    // ── 3. Register Handler ──────────────────────────────────────────────────

    public static class RegisterHandler implements HttpHandler {
        private final UserService userService;

        public RegisterHandler(UserService userService) {
            this.userService = userService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Optional<User> currentUser = WebSessionManager.getAuthenticatedUser(exchange);
                if (currentUser.isPresent()) {
                    User u = currentUser.get();
                    String dest = u.getRole() == Role.STUDENT ? "/student/dashboard" : "/alumni/dashboard";
                    HttpUtil.sendRedirect(exchange, dest);
                    return;
                }

                Map<String, String> query = HttpUtil.parseQueryParams(exchange.getRequestURI());
                String role = query.getOrDefault("role", "STUDENT");
                String html = WebViews.renderRegister(null, role, Collections.emptyMap());
                HttpUtil.sendHtml(exchange, 200, html);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> form = HttpUtil.parseFormData(exchange);
                String roleStr = form.getOrDefault("role", "STUDENT").trim();
                Role role;
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    role = Role.STUDENT;
                }

                RegistrationRequest req = new RegistrationRequest();
                req.setRole(role);
                req.setFullName(form.getOrDefault("fullName", "").trim());
                req.setEmail(form.getOrDefault("email", "").trim());
                req.setPassword(form.getOrDefault("password", ""));
                req.setConfirmPassword(form.getOrDefault("confirmPassword", ""));
                req.setPhone(form.getOrDefault("phone", "").trim());
                req.setDepartment(form.getOrDefault("department", "").trim());

                try {
                    String gradYr = form.get("graduationYear");
                    if (gradYr != null && !gradYr.isBlank()) {
                        req.setGraduationYear(Integer.parseInt(gradYr.trim()));
                    }
                } catch (NumberFormatException ignored) {}

                req.setDegree(form.getOrDefault("degree", "B.Tech").trim());

                if (role == Role.STUDENT) {
                    try {
                        String sem = form.get("currentSemester");
                        if (sem != null && !sem.isBlank()) {
                            req.setCurrentSemester(Integer.parseInt(sem.trim()));
                        }
                    } catch (NumberFormatException ignored) {}
                    req.setEnrollmentNumber(form.getOrDefault("enrollmentNumber", "").trim());
                    req.setSkills(form.getOrDefault("skills", "").trim());
                } else {
                    req.setCurrentCompany(form.getOrDefault("currentCompany", "").trim());
                    req.setCurrentRole(form.getOrDefault("currentRole", "").trim());
                    req.setIndustry(form.getOrDefault("industry", "").trim());
                    try {
                        String exp = form.get("yearsOfExperience");
                        if (exp != null && !exp.isBlank()) {
                            req.setYearsOfExperience(Integer.parseInt(exp.trim()));
                        }
                    } catch (NumberFormatException ignored) {}
                    req.setExpertiseAreas(form.getOrDefault("expertiseAreas", "").trim());
                }

                RegistrationResult result = userService.register(req);
                if (!result.isSuccess()) {
                    String html = WebViews.renderRegister(result.getMessage(), role.name(), form);
                    HttpUtil.sendHtml(exchange, 200, html);
                    return;
                }

                String successMsg = URLEncoder.encode("Account registered successfully! Please sign in.", StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/login?success=" + successMsg + "&role=" + role.name());
                return;
            }

            HttpUtil.sendText(exchange, 405, "Method Not Allowed");
        }
    }

    // ── 4. Logout Handler ────────────────────────────────────────────────────

    public static class LogoutHandler implements HttpHandler {
        private final UserService userService;

        public LogoutHandler(UserService userService) {
            this.userService = userService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            WebSessionManager.destroySession(exchange);
            userService.logout();
            String msg = URLEncoder.encode("You have been signed out.", StandardCharsets.UTF_8);
            HttpUtil.sendRedirect(exchange, "/login?success=" + msg);
        }
    }

    // ── 5. Student Dashboard Handler ─────────────────────────────────────────

    public static class StudentDashboardHandler implements HttpHandler {
        private final StudentProfileService studentProfileService;
        private final AlumniDirectoryService alumniDirectoryService;
        private final MentorshipRequestService mentorshipRequestService;

        public StudentDashboardHandler(StudentProfileService studentProfileService,
                                       AlumniDirectoryService alumniDirectoryService,
                                       MentorshipRequestService mentorshipRequestService) {
            this.studentProfileService = studentProfileService;
            this.alumniDirectoryService = alumniDirectoryService;
            this.mentorshipRequestService = mentorshipRequestService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Optional<User> authUser = WebSessionManager.getAuthenticatedUser(exchange);
            if (authUser.isEmpty()) {
                String err = URLEncoder.encode("Please log in to access the Student Dashboard.", StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/login?error=" + err + "&role=STUDENT");
                return;
            }

            User user = authUser.get();
            if (user.getRole() != Role.STUDENT) {
                HttpUtil.sendHtml(exchange, 403, "<h1>403 Forbidden</h1><p>Access denied: Student access only.</p><a href='/alumni/dashboard'>Go to Alumni Dashboard</a>");
                return;
            }

            Map<String, String> query = HttpUtil.parseQueryParams(exchange.getRequestURI());
            String searchKeyword = query.get("search");
            String successMsg = query.get("msg");
            String errorMsg = query.get("err");

            try {
                WebSessionManager.executeAs(user, () -> {
                    // 1. Student profile
                    StudentProfileResult profileRes = studentProfileService.viewMyProfile();
                    Student student = profileRes.isSuccess() ? profileRes.getStudent() : null;

                    // 2. Alumni list
                    List<Alumni> alumniList;
                    if (searchKeyword != null && !searchKeyword.isBlank()) {
                        AlumniDirectoryResult dirRes = alumniDirectoryService.searchAlumni(searchKeyword.trim());
                        alumniList = dirRes.isSuccess() ? dirRes.getAlumniList() : Collections.emptyList();
                    } else {
                        AlumniDirectoryResult dirRes = alumniDirectoryService.getAlumniDirectory();
                        alumniList = dirRes.isSuccess() ? dirRes.getAlumniList() : Collections.emptyList();
                    }

                    // 3. Sent Requests
                    MentorshipRequestResult reqRes = mentorshipRequestService.getMyRequests();
                    List<MentorshipRequest> myRequests = reqRes.isSuccess() ? reqRes.getRequests() : Collections.emptyList();

                    // 4. Accepted Connections
                    MentorshipRequestResult connRes = mentorshipRequestService.getMyMentorshipConnections();
                    List<MentorshipRequest> connections = connRes.isSuccess() ? connRes.getRequests() : Collections.emptyList();

                    String html = WebViews.renderStudentDashboard(
                            user, student, alumniList, searchKeyword, myRequests, connections, successMsg, errorMsg
                    );
                    HttpUtil.sendHtml(exchange, 200, html);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering student dashboard", e);
                HttpUtil.sendHtml(exchange, 500, "<h1>500 Internal Server Error</h1><p>" + HttpUtil.escapeHtml(e.getMessage()) + "</p>");
            }
        }
    }

    // ── 6. Student Send Request Handler ──────────────────────────────────────

    public static class StudentSendRequestHandler implements HttpHandler {
        private final MentorshipRequestService mentorshipRequestService;

        public StudentSendRequestHandler(MentorshipRequestService mentorshipRequestService) {
            this.mentorshipRequestService = mentorshipRequestService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            Optional<User> authUser = WebSessionManager.getAuthenticatedUser(exchange);
            if (authUser.isEmpty() || authUser.get().getRole() != Role.STUDENT) {
                HttpUtil.sendRedirect(exchange, "/login");
                return;
            }

            User user = authUser.get();
            Map<String, String> form = HttpUtil.parseFormData(exchange);
            String alumniIdStr = form.get("alumniId");
            String topic = form.getOrDefault("topic", "").trim();
            String message = form.getOrDefault("message", "").trim();

            int alumniId;
            try {
                alumniId = Integer.parseInt(alumniIdStr);
            } catch (Exception e) {
                String err = URLEncoder.encode("Invalid alumni selected.", StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/student/dashboard?err=" + err);
                return;
            }

            try {
                MentorshipRequestResult result = WebSessionManager.executeAs(user, () ->
                        mentorshipRequestService.sendRequest(alumniId, topic, message)
                );

                if (result.isSuccess()) {
                    String msg = URLEncoder.encode("Mentorship request sent successfully!", StandardCharsets.UTF_8);
                    HttpUtil.sendRedirect(exchange, "/student/dashboard?msg=" + msg);
                } else {
                    String err = URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
                    HttpUtil.sendRedirect(exchange, "/student/dashboard?err=" + err);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error sending mentorship request", e);
                String err = URLEncoder.encode("Failed to send request: " + e.getMessage(), StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/student/dashboard?err=" + err);
            }
        }
    }

    // ── 7. Alumni Dashboard Handler ──────────────────────────────────────────

    public static class AlumniDashboardHandler implements HttpHandler {
        private final AlumniProfileService alumniProfileService;
        private final MentorshipRequestService mentorshipRequestService;

        public AlumniDashboardHandler(AlumniProfileService alumniProfileService,
                                      MentorshipRequestService mentorshipRequestService) {
            this.alumniProfileService = alumniProfileService;
            this.mentorshipRequestService = mentorshipRequestService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Optional<User> authUser = WebSessionManager.getAuthenticatedUser(exchange);
            if (authUser.isEmpty()) {
                String err = URLEncoder.encode("Please log in to access the Alumni Dashboard.", StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/login?error=" + err + "&role=ALUMNI");
                return;
            }

            User user = authUser.get();
            if (user.getRole() != Role.ALUMNI) {
                HttpUtil.sendHtml(exchange, 403, "<h1>403 Forbidden</h1><p>Access denied: Alumni access only.</p><a href='/student/dashboard'>Go to Student Dashboard</a>");
                return;
            }

            Map<String, String> query = HttpUtil.parseQueryParams(exchange.getRequestURI());
            String successMsg = query.get("msg");
            String errorMsg = query.get("err");

            try {
                WebSessionManager.executeAs(user, () -> {
                    // 1. Alumni profile
                    AlumniProfileResult profileRes = alumniProfileService.viewMyProfile();
                    Alumni alumni = profileRes.isSuccess() ? profileRes.getAlumni() : null;

                    // 2. Incoming Requests
                    MentorshipRequestResult reqRes = mentorshipRequestService.getReceivedRequests();
                    List<MentorshipRequest> incomingRequests = reqRes.isSuccess() ? reqRes.getRequests() : Collections.emptyList();

                    // 3. Accepted Mentees
                    MentorshipRequestResult connRes = mentorshipRequestService.getMyMentorshipConnections();
                    List<MentorshipRequest> mentees = connRes.isSuccess() ? connRes.getRequests() : Collections.emptyList();

                    String html = WebViews.renderAlumniDashboard(
                            user, alumni, incomingRequests, mentees, successMsg, errorMsg
                    );
                    HttpUtil.sendHtml(exchange, 200, html);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering alumni dashboard", e);
                HttpUtil.sendHtml(exchange, 500, "<h1>500 Internal Server Error</h1><p>" + HttpUtil.escapeHtml(e.getMessage()) + "</p>");
            }
        }
    }

    // ── 8. Alumni Action Handler (Accept / Reject) ───────────────────────────

    public static class AlumniActionHandler implements HttpHandler {
        private final MentorshipRequestService mentorshipRequestService;

        public AlumniActionHandler(MentorshipRequestService mentorshipRequestService) {
            this.mentorshipRequestService = mentorshipRequestService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            Optional<User> authUser = WebSessionManager.getAuthenticatedUser(exchange);
            if (authUser.isEmpty() || authUser.get().getRole() != Role.ALUMNI) {
                HttpUtil.sendRedirect(exchange, "/login");
                return;
            }

            User user = authUser.get();
            Map<String, String> form = HttpUtil.parseFormData(exchange);
            String requestIdStr = form.get("requestId");
            String action = form.getOrDefault("action", "").trim().toLowerCase();

            int requestId;
            try {
                requestId = Integer.parseInt(requestIdStr);
            } catch (Exception e) {
                String err = URLEncoder.encode("Invalid mentorship request ID.", StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/alumni/dashboard?err=" + err);
                return;
            }

            try {
                MentorshipRequestResult result = WebSessionManager.executeAs(user, () -> {
                    if ("accept".equals(action)) {
                        return mentorshipRequestService.acceptRequest(requestId);
                    } else if ("reject".equals(action)) {
                        return mentorshipRequestService.rejectRequest(requestId);
                    } else {
                        return MentorshipRequestResult.failure("Unknown request action: " + action);
                    }
                });

                if (result.isSuccess()) {
                    String actionText = "accept".equals(action) ? "accepted" : "rejected";
                    String msg = URLEncoder.encode("Mentorship request #" + requestId + " successfully " + actionText + ".", StandardCharsets.UTF_8);
                    HttpUtil.sendRedirect(exchange, "/alumni/dashboard?msg=" + msg);
                } else {
                    String err = URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
                    HttpUtil.sendRedirect(exchange, "/alumni/dashboard?err=" + err);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing alumni action", e);
                String err = URLEncoder.encode("Failed to process action: " + e.getMessage(), StandardCharsets.UTF_8);
                HttpUtil.sendRedirect(exchange, "/alumni/dashboard?err=" + err);
            }
        }
    }
}

