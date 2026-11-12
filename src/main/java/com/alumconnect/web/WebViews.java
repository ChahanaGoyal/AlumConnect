package com.alumconnect.web;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTML view templates for the AlumConnect browser interface.
 * Generates self-contained, responsive, accessible HTML and CSS pages.
 */
public final class WebViews {

    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Formats a {@link LocalDateTime} into a consistent user-facing string using
     * Indian Standard Time (Asia/Kolkata).
     *
     * @param ldt timestamp to format
     * @return formatted date string, or "-" if null
     */
    public static String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return "-";
        }
        return ldt.format(DATE_FMT);
    }

    private WebViews() {
        // Prevent instantiation
    }

    // ── Shared CSS & Layout ──────────────────────────────────────────────────

    private static String getCommonStyles() {
        return """
            :root {
              --primary: #2563eb;
              --primary-hover: #1d4ed8;
              --primary-light: #eff6ff;
              --text-main: #0f172a;
              --text-muted: #64748b;
              --bg-page: #f8fafc;
              --card-bg: #ffffff;
              --border: #e2e8f0;
              --radius: 8px;
              --shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.05);
              --success: #16a34a;
              --success-bg: #dcfce7;
              --danger: #dc2626;
              --danger-bg: #fee2e2;
              --warning: #d97706;
              --warning-bg: #fef3c7;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
            body { background-color: var(--bg-page); color: var(--text-main); line-height: 1.5; min-height: 100vh; display: flex; flex-direction: column; }
            a { color: var(--primary); text-decoration: none; font-weight: 500; }
            a:hover { text-decoration: underline; }
            .container { max-width: 1100px; margin: 0 auto; padding: 24px 16px; width: 100%; flex: 1; }
            
            /* Navbar */
            header { background: #ffffff; border-bottom: 1px solid var(--border); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            .nav-inner { max-width: 1100px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; }
            .brand { font-size: 1.35rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 8px; text-decoration: none; }
            .brand:hover { text-decoration: none; }
            .nav-links { display: flex; align-items: center; gap: 16px; }
            .nav-links a { font-size: 0.95rem; color: var(--text-muted); }
            .nav-links a:hover { color: var(--primary); text-decoration: none; }
            .user-badge { background: var(--primary-light); color: var(--primary); font-size: 0.85rem; font-weight: 600; padding: 4px 10px; border-radius: 999px; }

            /* Buttons */
            .btn { display: inline-flex; align-items: center; justify-content: center; padding: 8px 16px; border-radius: var(--radius); font-weight: 600; font-size: 0.95rem; cursor: pointer; border: 1px solid transparent; transition: all 0.15s ease-in-out; text-decoration: none; }
            .btn:hover { text-decoration: none; }
            .btn-primary { background: var(--primary); color: #ffffff; }
            .btn-primary:hover { background: var(--primary-hover); }
            .btn-outline { border-color: var(--border); color: var(--text-main); background: #ffffff; }
            .btn-outline:hover { background: var(--bg-page); }
            .btn-danger { background: var(--danger); color: #ffffff; }
            .btn-danger:hover { background: #b91c1c; }
            .btn-success { background: var(--success); color: #ffffff; }
            .btn-success:hover { background: #15803d; }
            .btn-sm { padding: 4px 10px; font-size: 0.85rem; }

            /* Cards */
            .card { background: var(--card-bg); border-radius: var(--radius); border: 1px solid var(--border); box-shadow: var(--shadow); padding: 24px; margin-bottom: 24px; }
            .card-title { font-size: 1.25rem; font-weight: 700; margin-bottom: 16px; color: var(--text-main); display: flex; align-items: center; justify-content: space-between; }

            /* Forms */
            .form-group { margin-bottom: 16px; }
            label { display: block; font-size: 0.9rem; font-weight: 600; margin-bottom: 6px; color: var(--text-main); }
            input[type="text"], input[type="email"], input[type="password"], input[type="number"], select, textarea {
              width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--radius); font-size: 0.95rem; background: #ffffff; color: var(--text-main);
            }
            input:focus, select:focus, textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15); }
            .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
            .form-hint { font-size: 0.8rem; color: var(--text-muted); margin-top: 4px; }

            /* Alerts */
            .alert { padding: 12px 16px; border-radius: var(--radius); margin-bottom: 20px; font-size: 0.95rem; display: flex; align-items: center; gap: 8px; }
            .alert-danger { background: var(--danger-bg); color: #991b1b; border: 1px solid #fca5a5; }
            .alert-success { background: var(--success-bg); color: #166534; border: 1px solid #86efac; }
            .alert-info { background: var(--primary-light); color: #1e40af; border: 1px solid #93c5fd; }

            /* Tables */
            .table-wrap { overflow-x: auto; margin-top: 12px; }
            table { width: 100%; border-collapse: collapse; text-align: left; font-size: 0.92rem; }
            th { background: #f1f5f9; padding: 12px 14px; font-weight: 600; color: var(--text-muted); border-bottom: 1px solid var(--border); }
            td { padding: 12px 14px; border-bottom: 1px solid var(--border); vertical-align: middle; }
            tr:hover td { background-color: #f8fafc; }

            /* Badges */
            .badge { display: inline-block; padding: 3px 8px; border-radius: 999px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
            .badge-pending { background: var(--warning-bg); color: var(--warning); }
            .badge-accepted { background: var(--success-bg); color: var(--success); }
            .badge-rejected { background: var(--danger-bg); color: var(--danger); }
            .badge-active { background: #dbeafe; color: #1d4ed8; }

            /* Footer */
            footer { background: #ffffff; border-top: 1px solid var(--border); padding: 16px 0; text-align: center; color: var(--text-muted); font-size: 0.85rem; }
            """;
    }

    private static String renderNavbar(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("<header><div class=\"nav-inner\">");
        sb.append("<a href=\"/\" class=\"brand\">🎓 AlumConnect</a>");
        sb.append("<div class=\"nav-links\">");
        sb.append("<a href=\"/\">Home</a>");
        if (user != null) {
            String dashUrl = user.getRole() == Role.STUDENT ? "/student/dashboard" : "/alumni/dashboard";
            sb.append("<a href=\"").append(dashUrl).append("\">Dashboard</a>");
            sb.append("<span class=\"user-badge\">👤 ").append(HttpUtil.escapeHtml(user.getFullName()))
                    .append(" (").append(user.getRole()).append(")</span>");
            sb.append("<a href=\"/logout\" class=\"btn btn-outline btn-sm\">Logout</a>");
        } else {
            sb.append("<a href=\"/login\" class=\"btn btn-outline btn-sm\">Login</a>");
            sb.append("<a href=\"/register\" class=\"btn btn-primary btn-sm\">Register</a>");
        }
        sb.append("</div></div></header>");
        return sb.toString();
    }

    private static String renderFooter() {
        return "<footer><div class=\"container\">&copy; 2026 AlumConnect Platform &bull; Connecting Students and Alumni</div></footer>";
    }

    private static String renderPage(String title, User user, String content) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>" + HttpUtil.escapeHtml(title) + " - AlumConnect</title>" +
                "<style>" + getCommonStyles() + "</style>" +
                "</head><body>" +
                renderNavbar(user) +
                "<main class=\"container\">" + content + "</main>" +
                renderFooter() +
                "</body></html>";
    }

    // ── Home Page ────────────────────────────────────────────────────────────

    public static String renderHome(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card\" style=\"text-align: center; padding: 48px 24px; background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);\">");
        sb.append("<h1 style=\"font-size: 2.2rem; font-weight: 800; color: #1e3a8a; margin-bottom: 12px;\">Welcome to AlumConnect</h1>");
        sb.append("<p style=\"font-size: 1.15rem; color: #475569; max-width: 650px; margin: 0 auto 28px;\">The premier alumni networking and mentorship platform connecting ambitious students with experienced graduates.</p>");
        sb.append("<div style=\"display: flex; gap: 12px; justify-content: center; flex-wrap: wrap;\">");

        if (user != null) {
            String dashUrl = user.getRole() == Role.STUDENT ? "/student/dashboard" : "/alumni/dashboard";
            sb.append("<a href=\"").append(dashUrl).append("\" class=\"btn btn-primary\" style=\"padding: 10px 24px; font-size: 1rem;\">Go to My Dashboard &rarr;</a>");
        } else {
            sb.append("<a href=\"/login\" class=\"btn btn-primary\" style=\"padding: 10px 24px; font-size: 1rem;\">Login to Platform</a>");
            sb.append("<a href=\"/register\" class=\"btn btn-outline\" style=\"padding: 10px 24px; font-size: 1rem;\">Register New Account</a>");
        }
        sb.append("</div></div>");

        sb.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px;\">");

        sb.append("<div class=\"card\">");
        sb.append("<div style=\"font-size: 2rem; margin-bottom: 8px;\">🎓</div>");
        sb.append("<h3 style=\"margin-bottom: 8px;\">For Students</h3>");
        sb.append("<p style=\"color: var(--text-muted); font-size: 0.95rem;\">Discover verified alumni from your college. Search by industry, company, and skills. Send mentorship requests with a tailored message and track your progress.</p>");
        sb.append("</div>");

        sb.append("<div class=\"card\">");
        sb.append("<div style=\"font-size: 2rem; margin-bottom: 8px;\">💼</div>");
        sb.append("<h3 style=\"margin-bottom: 8px;\">For Alumni</h3>");
        sb.append("<p style=\"color: var(--text-muted); font-size: 0.95rem;\">Empower current students through 1-on-1 mentorship. Review incoming requests, share career advice, and connect with fellow alumni in your industry.</p>");
        sb.append("</div>");

        sb.append("<div class=\"card\">");
        sb.append("<div style=\"font-size: 2rem; margin-bottom: 8px;\">🤝</div>");
        sb.append("<h3 style=\"margin-bottom: 8px;\">Structured Mentorship</h3>");
        sb.append("<p style=\"color: var(--text-muted); font-size: 0.95rem;\">Role-based security ensures transparent and controlled communication. Alumni can manage their mentee capacity and accept requests with a single click.</p>");
        sb.append("</div>");

        sb.append("</div>");

        return renderPage("Home", user, sb.toString());
    }

    // ── Login Page ───────────────────────────────────────────────────────────

    public static String renderLogin(String error, String success, String selectedRole, String email) {
        if (selectedRole == null || selectedRole.isBlank()) {
            selectedRole = "STUDENT";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 440px; margin: 40px auto;\">");
        sb.append("<div class=\"card\">");
        sb.append("<h2 style=\"text-align: center; margin-bottom: 6px;\">Sign In to AlumConnect</h2>");
        sb.append("<p style=\"text-align: center; color: var(--text-muted); font-size: 0.9rem; margin-bottom: 24px;\">Enter your credentials and choose your account role.</p>");

        if (error != null && !error.isBlank()) {
            sb.append("<div class=\"alert alert-danger\">⚠️ ").append(HttpUtil.escapeHtml(error)).append("</div>");
        }
        if (success != null && !success.isBlank()) {
            sb.append("<div class=\"alert alert-success\">✅ ").append(HttpUtil.escapeHtml(success)).append("</div>");
        }

        sb.append("<form method=\"POST\" action=\"/login\">");

        sb.append("<div class=\"form-group\">");
        sb.append("<label>Login As Role</label>");
        sb.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 8px;\">");

        boolean isStudent = !"ALUMNI".equalsIgnoreCase(selectedRole);
        sb.append("<label style=\"border: 1px solid var(--border); border-radius: var(--radius); padding: 10px; text-align: center; cursor: pointer; background: ")
                .append(isStudent ? "var(--primary-light); border-color: var(--primary);" : "#ffffff;")
                .append("\"><input type=\"radio\" name=\"role\" value=\"STUDENT\" ").append(isStudent ? "checked" : "")
                .append(" style=\"margin-right: 6px;\"> Student</label>");

        sb.append("<label style=\"border: 1px solid var(--border); border-radius: var(--radius); padding: 10px; text-align: center; cursor: pointer; background: ")
                .append(!isStudent ? "var(--primary-light); border-color: var(--primary);" : "#ffffff;")
                .append("\"><input type=\"radio\" name=\"role\" value=\"ALUMNI\" ").append(!isStudent ? "checked" : "")
                .append(" style=\"margin-right: 6px;\"> Alumni</label>");

        sb.append("</div></div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"email\">Email Address</label>");
        sb.append("<input type=\"email\" id=\"email\" name=\"email\" required placeholder=\"user@example.com\" value=\"")
                .append(HttpUtil.escapeHtml(email)).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"password\">Password</label>");
        sb.append("<input type=\"password\" id=\"password\" name=\"password\" required placeholder=\"••••••••\">");
        sb.append("</div>");

        sb.append("<button type=\"submit\" class=\"btn btn-primary\" style=\"width: 100%; padding: 10px; font-size: 1rem; margin-top: 8px;\">Sign In</button>");
        sb.append("</form>");

        sb.append("<div style=\"text-align: center; margin-top: 20px; font-size: 0.9rem; color: var(--text-muted);\">");
        sb.append("Don't have an account yet? <a href=\"/register\">Create an Account</a>");
        sb.append("</div>");

        sb.append("</div></div>");

        return renderPage("Login", null, sb.toString());
    }

    // ── Registration Page ────────────────────────────────────────────────────

    public static String renderRegister(String error, String selectedRole, Map<String, String> values) {
        if (selectedRole == null || selectedRole.isBlank()) {
            selectedRole = "STUDENT";
        }
        boolean isStudent = !"ALUMNI".equalsIgnoreCase(selectedRole);

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 680px; margin: 24px auto;\">");
        sb.append("<div class=\"card\">");
        sb.append("<h2 style=\"text-align: center; margin-bottom: 6px;\">Create Your AlumConnect Account</h2>");
        sb.append("<p style=\"text-align: center; color: var(--text-muted); font-size: 0.9rem; margin-bottom: 24px;\">Select your role to get started.</p>");

        if (error != null && !error.isBlank()) {
            sb.append("<div class=\"alert alert-danger\">⚠️ ").append(HttpUtil.escapeHtml(error)).append("</div>");
        }

        sb.append("<form method=\"POST\" action=\"/register\" id=\"regForm\">");

        // Role Selector Radio Buttons
        sb.append("<div class=\"form-group\">");
        sb.append("<label>Select Registration Role</label>");
        sb.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 12px;\">");
        sb.append("<label style=\"border: 2px solid ").append(isStudent ? "var(--primary)" : "var(--border)").append("; border-radius: var(--radius); padding: 12px; text-align: center; cursor: pointer; background: ")
                .append(isStudent ? "var(--primary-light)" : "#fff").append(";\">")
                .append("<input type=\"radio\" name=\"role\" value=\"STUDENT\" ").append(isStudent ? "checked" : "")
                .append(" onchange=\"document.getElementById('studentFields').style.display='block'; document.getElementById('alumniFields').style.display='none';\">")
                .append(" <strong>🎓 Student Account</strong></label>");

        sb.append("<label style=\"border: 2px solid ").append(!isStudent ? "var(--primary)" : "var(--border)").append("; border-radius: var(--radius); padding: 12px; text-align: center; cursor: pointer; background: ")
                .append(!isStudent ? "var(--primary-light)" : "#fff").append(";\">")
                .append("<input type=\"radio\" name=\"role\" value=\"ALUMNI\" ").append(!isStudent ? "checked" : "")
                .append(" onchange=\"document.getElementById('studentFields').style.display='none'; document.getElementById('alumniFields').style.display='block';\">")
                .append(" <strong>💼 Alumni Account</strong></label>");
        sb.append("</div></div>");

        // Common Core Fields
        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"fullName\">Full Name *</label>");
        sb.append("<input type=\"text\" id=\"fullName\" name=\"fullName\" required placeholder=\"John Doe\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("fullName", ""))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"email\">Email Address *</label>");
        sb.append("<input type=\"email\" id=\"email\" name=\"email\" required placeholder=\"user@example.com\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("email", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        // Password & Confirm Password
        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"password\">Password *</label>");
        sb.append("<input type=\"password\" id=\"password\" name=\"password\" required placeholder=\"Min 8 chars, 1 uppercase, 1 lowercase\">");
        sb.append("<div class=\"form-hint\">Must be &ge;8 characters with uppercase, lowercase, and digit/special char.</div>");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"confirmPassword\">Confirm Password *</label>");
        sb.append("<input type=\"password\" id=\"confirmPassword\" name=\"confirmPassword\" required placeholder=\"Repeat password\">");
        sb.append("</div>");
        sb.append("</div>");

        // Contact & Academic Core Fields
        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"phone\">Phone Number</label>");
        sb.append("<input type=\"text\" id=\"phone\" name=\"phone\" placeholder=\"+91 9876543210\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("phone", ""))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"department\">Department / Branch *</label>");
        sb.append("<input type=\"text\" id=\"department\" name=\"department\" required placeholder=\"Computer Science\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("department", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"graduationYear\">Graduation Year *</label>");
        sb.append("<input type=\"number\" id=\"graduationYear\" name=\"graduationYear\" required placeholder=\"2025\" min=\"1950\" max=\"2035\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("graduationYear", ""))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"degree\">Degree</label>");
        sb.append("<input type=\"text\" id=\"degree\" name=\"degree\" placeholder=\"B.Tech\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("degree", "B.Tech"))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        // Student Specific Fields
        sb.append("<div id=\"studentFields\" style=\"display: ").append(isStudent ? "block" : "none").append("; border-top: 1px solid var(--border); padding-top: 16px; margin-top: 8px;\">");
        sb.append("<h4 style=\"margin-bottom: 12px; color: var(--primary);\">Student Academic Details</h4>");
        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"currentSemester\">Current Semester (1-8)</label>");
        sb.append("<input type=\"number\" id=\"currentSemester\" name=\"currentSemester\" min=\"1\" max=\"8\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("currentSemester", "1"))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"enrollmentNumber\">Enrollment / Roll Number</label>");
        sb.append("<input type=\"text\" id=\"enrollmentNumber\" name=\"enrollmentNumber\" placeholder=\"EN2022001\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("enrollmentNumber", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"skills\">Skills &amp; Technologies</label>");
        sb.append("<input type=\"text\" id=\"skills\" name=\"skills\" placeholder=\"Java, Python, SQL, React\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("skills", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        // Alumni Specific Fields
        sb.append("<div id=\"alumniFields\" style=\"display: ").append(!isStudent ? "block" : "none").append("; border-top: 1px solid var(--border); padding-top: 16px; margin-top: 8px;\">");
        sb.append("<h4 style=\"margin-bottom: 12px; color: var(--primary);\">Alumni Professional Details</h4>");
        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"currentCompany\">Current Company *</label>");
        sb.append("<input type=\"text\" id=\"currentCompany\" name=\"currentCompany\" placeholder=\"Google, Microsoft, Infosys\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("currentCompany", ""))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"currentRole\">Current Job Role *</label>");
        sb.append("<input type=\"text\" id=\"currentRole\" name=\"currentRole\" placeholder=\"Software Engineer\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("currentRole", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div class=\"form-grid\">");
        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"industry\">Industry</label>");
        sb.append("<input type=\"text\" id=\"industry\" name=\"industry\" placeholder=\"Information Technology\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("industry", ""))).append("\">");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"yearsOfExperience\">Years of Experience</label>");
        sb.append("<input type=\"number\" id=\"yearsOfExperience\" name=\"yearsOfExperience\" min=\"0\" max=\"50\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("yearsOfExperience", "0"))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div class=\"form-group\">");
        sb.append("<label for=\"expertiseAreas\">Expertise Areas</label>");
        sb.append("<input type=\"text\" id=\"expertiseAreas\" name=\"expertiseAreas\" placeholder=\"Backend Architecture, Cloud Systems, Career Guidance\" value=\"")
                .append(HttpUtil.escapeHtml(values.getOrDefault("expertiseAreas", ""))).append("\">");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<button type=\"submit\" class=\"btn btn-primary\" style=\"width: 100%; padding: 12px; font-size: 1rem; margin-top: 16px;\">Create Account</button>");
        sb.append("</form>");

        sb.append("<div style=\"text-align: center; margin-top: 20px; font-size: 0.9rem; color: var(--text-muted);\">");
        sb.append("Already have an account? <a href=\"/login\">Sign in here</a>");
        sb.append("</div>");

        sb.append("</div></div>");

        return renderPage("Register", null, sb.toString());
    }

    // ── Student Dashboard ────────────────────────────────────────────────────

    public static String renderStudentDashboard(User user, Student student, List<Alumni> alumniList,
                                                String searchKeyword, List<MentorshipRequest> myRequests,
                                                List<MentorshipRequest> connections, String message, String error) {
        StringBuilder sb = new StringBuilder();

        if (message != null && !message.isBlank()) {
            sb.append("<div class=\"alert alert-success\">✅ ").append(HttpUtil.escapeHtml(message)).append("</div>");
        }
        if (error != null && !error.isBlank()) {
            sb.append("<div class=\"alert alert-danger\">⚠️ ").append(HttpUtil.escapeHtml(error)).append("</div>");
        }

        // Student Profile Header
        sb.append("<div class=\"card\" style=\"display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;\">");
        sb.append("<div>");
        sb.append("<h2 style=\"margin-bottom: 4px;\">Welcome, ").append(HttpUtil.escapeHtml(user.getFullName())).append("!</h2>");
        sb.append("<p style=\"color: var(--text-muted); font-size: 0.92rem;\">");
        sb.append("🎓 Student | Department: <strong>").append(HttpUtil.escapeHtml(student != null && student.getDepartment() != null ? student.getDepartment() : "General")).append("</strong>");
        if (student != null && student.getGraduationYear() > 0) {
            sb.append(" | Class of <strong>").append(student.getGraduationYear()).append("</strong>");
        }
        if (student != null && student.getCurrentSemester() > 0) {
            sb.append(" | Semester: <strong>").append(student.getCurrentSemester()).append("</strong>");
        }
        sb.append("</p></div>");
        sb.append("<div style=\"display: flex; gap: 8px;\">");
        sb.append("<a href=\"#directorySection\" class=\"btn btn-primary btn-sm\">Find Alumni</a>");
        sb.append("<a href=\"#requestsSection\" class=\"btn btn-outline btn-sm\">My Requests (").append(myRequests != null ? myRequests.size() : 0).append(")</a>");
        sb.append("</div></div>");

        // Section: Active Connections
        if (connections != null && !connections.isEmpty()) {
            sb.append("<div class=\"card\">");
            sb.append("<div class=\"card-title\"><span>🤝 My Connected Mentors (").append(connections.size()).append(")</span></div>");
            sb.append("<div class=\"table-wrap\"><table><thead><tr>");
            sb.append("<th>Mentor Name</th><th>Company &amp; Role</th><th>Department</th><th>Email</th><th>Topic</th><th>Connected Date</th>");
            sb.append("</tr></thead><tbody>");
            for (MentorshipRequest c : connections) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(c.getAlumniName() != null ? c.getAlumniName() : "Alumni #" + c.getAlumniId())).append("</strong></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(c.getAlumniRole() != null ? c.getAlumniRole() : "Professional"))
                        .append(" at ").append(HttpUtil.escapeHtml(c.getAlumniCompany() != null ? c.getAlumniCompany() : "Industry")).append("</td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(c.getAlumniDepartment() != null ? c.getAlumniDepartment() : "-")).append("</td>");
                sb.append("<td><a href=\"mailto:").append(HttpUtil.escapeHtml(c.getAlumniEmail() != null ? c.getAlumniEmail() : "")).append("\">")
                        .append(HttpUtil.escapeHtml(c.getAlumniEmail() != null ? c.getAlumniEmail() : "-")).append("</a></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(c.getTopic())).append("</td>");
                sb.append("<td>").append(formatDateTime(c.getRespondedAt())).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div></div>");
        }

        // Section: Browse & Search Alumni Directory
        sb.append("<div class=\"card\" id=\"directorySection\">");
        sb.append("<div class=\"card-title\"><span>🔍 Browse &amp; Search Alumni Directory</span></div>");
        sb.append("<form method=\"GET\" action=\"/student/dashboard\" style=\"display: flex; gap: 8px; margin-bottom: 20px;\">");
        sb.append("<input type=\"text\" name=\"search\" placeholder=\"Search by name, company, role, skills, or department...\" value=\"")
                .append(HttpUtil.escapeHtml(searchKeyword != null ? searchKeyword : "")).append("\" style=\"flex: 1;\">");
        sb.append("<button type=\"submit\" class=\"btn btn-primary\">Search</button>");
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            sb.append("<a href=\"/student/dashboard\" class=\"btn btn-outline\">Clear</a>");
        }
        sb.append("</form>");

        if (alumniList == null || alumniList.isEmpty()) {
            sb.append("<div style=\"text-align: center; padding: 24px; color: var(--text-muted);\">No alumni found matching your criteria.</div>");
        } else {
            sb.append("<div class=\"table-wrap\"><table><thead><tr>");
            sb.append("<th>Name</th><th>Role &amp; Company</th><th>Department &amp; Batch</th><th>Experience</th><th>Expertise</th><th>Status</th><th>Action</th>");
            sb.append("</tr></thead><tbody>");
            for (Alumni a : alumniList) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(a.getFullName())).append("</strong></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(a.getCurrentRole() != null ? a.getCurrentRole() : "Alumni"))
                        .append(" at <strong>").append(HttpUtil.escapeHtml(a.getCurrentCompany() != null ? a.getCurrentCompany() : "-")).append("</strong></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(a.getDepartment() != null ? a.getDepartment() : "-"))
                        .append(" (").append(a.getGraduationYear() > 0 ? a.getGraduationYear() : "-").append(")</td>");
                sb.append("<td>").append(a.getYearsOfExperience() >= 0 ? a.getYearsOfExperience() + " yrs" : "-").append("</td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(a.getExpertiseAreas() != null ? a.getExpertiseAreas() : "-")).append("</td>");
                sb.append("<td>");
                if (a.isAvailableForMentorship()) {
                    sb.append("<span class=\"badge badge-accepted\">Available</span>");
                } else {
                    sb.append("<span class=\"badge badge-rejected\">Unavailable</span>");
                }
                sb.append("</td>");
                sb.append("<td>");
                if (a.isAvailableForMentorship()) {
                    sb.append("<button class=\"btn btn-primary btn-sm\" onclick=\"openRequestModal(")
                            .append(a.getId()).append(", '")
                            .append(HttpUtil.escapeHtml(a.getFullName()).replace("'", "\\'")).append("')\">Request</button>");
                } else {
                    sb.append("<span style=\"color: var(--text-muted); font-size: 0.85rem;\">Full</span>");
                }
                sb.append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div>");
        }
        sb.append("</div>");

        // Section: Sent Mentorship Requests
        sb.append("<div class=\"card\" id=\"requestsSection\">");
        sb.append("<div class=\"card-title\"><span>📨 My Sent Mentorship Requests</span></div>");
        if (myRequests == null || myRequests.isEmpty()) {
            sb.append("<div style=\"text-align: center; padding: 20px; color: var(--text-muted);\">You have not sent any mentorship requests yet.</div>");
        } else {
            sb.append("<div class=\"table-wrap\"><table><thead><tr>");
            sb.append("<th>ID</th><th>Mentor</th><th>Topic</th><th>Message</th><th>Sent Date</th><th>Status</th><th>Response Note</th>");
            sb.append("</tr></thead><tbody>");
            for (MentorshipRequest r : myRequests) {
                sb.append("<tr>");
                sb.append("<td>#").append(r.getId()).append("</td>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(r.getAlumniName() != null ? r.getAlumniName() : "Alumni #" + r.getAlumniId())).append("</strong></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(r.getTopic())).append("</td>");
                sb.append("<td style=\"max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;\">")
                        .append(HttpUtil.escapeHtml(r.getMessage())).append("</td>");
                sb.append("<td>").append(formatDateTime(r.getRequestedAt())).append("</td>");
                sb.append("<td>");
                if (r.getStatus() == MentorshipStatus.PENDING) {
                    sb.append("<span class=\"badge badge-pending\">Pending</span>");
                } else if (r.getStatus() == MentorshipStatus.ACCEPTED) {
                    sb.append("<span class=\"badge badge-accepted\">Accepted</span>");
                } else {
                    sb.append("<span class=\"badge badge-rejected\">Rejected</span>");
                }
                sb.append("</td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(r.getResponseNotes() != null ? r.getResponseNotes() : "-")).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div>");
        }
        sb.append("</div>");

        // Mentorship Request Modal (simple native JavaScript overlay)
        sb.append("""
            <div id="requestModal" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.5); z-index:1000; align-items:center; justify-content:center;">
              <div class="card" style="width:100%; max-width:500px; margin:20px;">
                <div class="card-title">
                  <span>Send Mentorship Request</span>
                  <button type="button" class="btn btn-outline btn-sm" onclick="closeRequestModal()">&times;</button>
                </div>
                <form method="POST" action="/student/send-request">
                  <input type="hidden" name="alumniId" id="modalAlumniId">
                  <p style="margin-bottom: 12px; font-size: 0.95rem;">Requesting mentorship from <strong id="modalAlumniName"></strong></p>
                  <div class="form-group">
                    <label for="topic">Topic / Purpose *</label>
                    <input type="text" id="topic" name="topic" required placeholder="e.g. Career guidance in Cloud/DevOps">
                  </div>
                  <div class="form-group">
                    <label for="message">Message to Mentor *</label>
                    <textarea id="message" name="message" rows="4" required placeholder="Introduce yourself, describe your background, and share what you hope to learn..."></textarea>
                  </div>
                  <div style="display:flex; justify-content:flex-end; gap:8px;">
                    <button type="button" class="btn btn-outline" onclick="closeRequestModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Submit Request</button>
                  </div>
                </form>
              </div>
            </div>
            <script>
              function openRequestModal(alumniId, alumniName) {
                document.getElementById('modalAlumniId').value = alumniId;
                document.getElementById('modalAlumniName').innerText = alumniName;
                var modal = document.getElementById('requestModal');
                modal.style.display = 'flex';
              }
              function closeRequestModal() {
                document.getElementById('requestModal').style.display = 'none';
              }
            </script>
            """);

        return renderPage("Student Dashboard", user, sb.toString());
    }

    // ── Alumni Dashboard ─────────────────────────────────────────────────────

    public static String renderAlumniDashboard(User user, Alumni alumni, List<MentorshipRequest> incomingRequests,
                                               List<MentorshipRequest> mentees, String message, String error) {
        StringBuilder sb = new StringBuilder();

        if (message != null && !message.isBlank()) {
            sb.append("<div class=\"alert alert-success\">✅ ").append(HttpUtil.escapeHtml(message)).append("</div>");
        }
        if (error != null && !error.isBlank()) {
            sb.append("<div class=\"alert alert-danger\">⚠️ ").append(HttpUtil.escapeHtml(error)).append("</div>");
        }

        // Alumni Profile Header
        sb.append("<div class=\"card\" style=\"display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;\">");
        sb.append("<div>");
        sb.append("<h2 style=\"margin-bottom: 4px;\">Welcome, ").append(HttpUtil.escapeHtml(user.getFullName())).append("!</h2>");
        sb.append("<p style=\"color: var(--text-muted); font-size: 0.92rem;\">");
        sb.append("💼 Alumni | <strong>").append(HttpUtil.escapeHtml(alumni != null && alumni.getCurrentRole() != null ? alumni.getCurrentRole() : "Professional"))
                .append("</strong> at <strong>").append(HttpUtil.escapeHtml(alumni != null && alumni.getCurrentCompany() != null ? alumni.getCurrentCompany() : "Company")).append("</strong>");
        if (alumni != null && alumni.getYearsOfExperience() >= 0) {
            sb.append(" | ").append(alumni.getYearsOfExperience()).append(" yrs experience");
        }
        if (alumni != null && alumni.getGraduationYear() > 0) {
            sb.append(" | Class of ").append(alumni.getGraduationYear());
        }
        sb.append("</p></div>");

        sb.append("<div>");
        int menteeCount = mentees != null ? mentees.size() : 0;
        int maxMentees = alumni != null && alumni.getMaxMentees() >= 0 ? alumni.getMaxMentees() : 3;
        sb.append("<span class=\"badge badge-active\" style=\"font-size: 0.9rem; padding: 6px 14px;\">Mentees: ")
                .append(menteeCount).append(" / ").append(maxMentees).append("</span>");
        sb.append("</div></div>");

        // Section: Incoming Mentorship Requests (PENDING only)
        // Filter to only show PENDING requests in this section; accepted/rejected
        // requests are shown in the Active Mentees & Connections section below.
        List<MentorshipRequest> pendingIncoming = incomingRequests != null
                ? incomingRequests.stream()
                        .filter(r -> r.getStatus() == MentorshipStatus.PENDING)
                        .toList()
                : Collections.emptyList();

        sb.append("<div class=\"card\">");
        sb.append("<div class=\"card-title\"><span>📬 Incoming Mentorship Requests (").append(pendingIncoming.size()).append(")</span></div>");

        if (pendingIncoming.isEmpty()) {
            sb.append("<div style=\"text-align: center; padding: 24px; color: var(--text-muted);\">No pending mentorship requests at this time.</div>");
        } else {
            sb.append("<div class=\"table-wrap\"><table><thead><tr>");
            sb.append("<th>ID</th><th>Student Name</th><th>Department &amp; Year</th><th>Topic</th><th>Message</th><th>Received Date</th><th>Actions</th>");
            sb.append("</tr></thead><tbody>");
            for (MentorshipRequest req : pendingIncoming) {
                sb.append("<tr>");
                sb.append("<td>#").append(req.getId()).append("</td>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(req.getStudentName() != null ? req.getStudentName() : "Student #" + req.getStudentId())).append("</strong><br>")
                        .append("<span style=\"font-size:0.8rem; color:var(--text-muted);\">").append(HttpUtil.escapeHtml(req.getStudentEmail() != null ? req.getStudentEmail() : "")).append("</span></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(req.getStudentDepartment() != null ? req.getStudentDepartment() : "-"))
                        .append(" (").append(req.getStudentGraduationYear() != null ? req.getStudentGraduationYear() : "-").append(")</td>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(req.getTopic())).append("</strong></td>");
                sb.append("<td style=\"max-width: 250px;\">").append(HttpUtil.escapeHtml(req.getMessage())).append("</td>");
                sb.append("<td>").append(formatDateTime(req.getRequestedAt())).append("</td>");
                sb.append("<td style=\"white-space: nowrap;\">");
                sb.append("<div style=\"display: flex; gap: 6px;\">");

                // Accept form — only rendered for PENDING requests
                sb.append("<form method=\"POST\" action=\"/alumni/request-action\" style=\"display:inline;\">");
                sb.append("<input type=\"hidden\" name=\"requestId\" value=\"").append(req.getId()).append("\">");
                sb.append("<input type=\"hidden\" name=\"action\" value=\"accept\">");
                sb.append("<button type=\"submit\" class=\"btn btn-success btn-sm\">Accept</button>");
                sb.append("</form>");

                // Reject form — only rendered for PENDING requests
                sb.append("<form method=\"POST\" action=\"/alumni/request-action\" style=\"display:inline;\">");
                sb.append("<input type=\"hidden\" name=\"requestId\" value=\"").append(req.getId()).append("\">");
                sb.append("<input type=\"hidden\" name=\"action\" value=\"reject\">");
                sb.append("<button type=\"submit\" class=\"btn btn-danger btn-sm\">Reject</button>");
                sb.append("</form>");

                sb.append("</div></td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div>");
        }
        sb.append("</div>");

        // Section: Active Mentees / Connections
        sb.append("<div class=\"card\">");
        sb.append("<div class=\"card-title\"><span>👥 Active Mentees &amp; Connections (").append(menteeCount).append(")</span></div>");

        if (mentees == null || mentees.isEmpty()) {
            sb.append("<div style=\"text-align: center; padding: 24px; color: var(--text-muted);\">No active mentees yet. Accepted requests will appear here.</div>");
        } else {
            sb.append("<div class=\"table-wrap\"><table><thead><tr>");
            sb.append("<th>Student Name</th><th>Email</th><th>Department &amp; Batch</th><th>Mentorship Topic</th><th>Accepted Date</th>");
            sb.append("</tr></thead><tbody>");
            for (MentorshipRequest m : mentees) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(HttpUtil.escapeHtml(m.getStudentName() != null ? m.getStudentName() : "Student #" + m.getStudentId())).append("</strong></td>");
                sb.append("<td><a href=\"mailto:").append(HttpUtil.escapeHtml(m.getStudentEmail() != null ? m.getStudentEmail() : "")).append("\">")
                        .append(HttpUtil.escapeHtml(m.getStudentEmail() != null ? m.getStudentEmail() : "-")).append("</a></td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(m.getStudentDepartment() != null ? m.getStudentDepartment() : "-"))
                        .append(" (").append(m.getStudentGraduationYear() != null ? m.getStudentGraduationYear() : "-").append(")</td>");
                sb.append("<td>").append(HttpUtil.escapeHtml(m.getTopic())).append("</td>");
                sb.append("<td>").append(formatDateTime(m.getRespondedAt())).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div>");
        }
        sb.append("</div>");

        return renderPage("Alumni Dashboard", user, sb.toString());
    }
}

