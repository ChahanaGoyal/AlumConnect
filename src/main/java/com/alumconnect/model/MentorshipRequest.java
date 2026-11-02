package com.alumconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing a row in the {@code mentorship_requests} table.
 * Mirrors the database schema defined in schema.sql, and optionally holds
 * associated student or alumni information populated via table joins.
 */
public class MentorshipRequest {
    private Integer id; // AUTO_INCREMENT primary key
    private int studentId; // FK to users.id (student)
    private int alumniId; // FK to users.id (alumni)
    private String topic;
    private String message;
    private MentorshipStatus status = MentorshipStatus.PENDING;
    private String responseNotes;
    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    // Associated student information (populated on retrieval with user/profile join for alumni view)
    private String studentName;
    private String studentEmail;
    private String studentDegree;
    private String studentDepartment;
    private Integer studentGraduationYear;

    // Associated alumni information (populated on retrieval with user/profile join for student view)
    private String alumniName;
    private String alumniEmail;
    private String alumniDegree;
    private String alumniDepartment;
    private String alumniCompany;
    private String alumniRole;

    public MentorshipRequest() {
        // Required for JDBC mapping
    }

    public MentorshipRequest(int studentId, int alumniId, String topic, String message) {
        this.studentId = studentId;
        this.alumniId = alumniId;
        this.topic = Objects.requireNonNull(topic);
        this.message = Objects.requireNonNull(message);
        this.status = MentorshipStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getAlumniId() { return alumniId; }
    public void setAlumniId(int alumniId) { this.alumniId = alumniId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = Objects.requireNonNull(topic); }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = Objects.requireNonNull(message); }

    public MentorshipStatus getStatus() { return status; }
    public void setStatus(MentorshipStatus status) { this.status = Objects.requireNonNull(status); }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentDegree() { return studentDegree; }
    public void setStudentDegree(String studentDegree) { this.studentDegree = studentDegree; }

    public String getStudentDepartment() { return studentDepartment; }
    public void setStudentDepartment(String studentDepartment) { this.studentDepartment = studentDepartment; }

    public Integer getStudentGraduationYear() { return studentGraduationYear; }
    public void setStudentGraduationYear(Integer studentGraduationYear) { this.studentGraduationYear = studentGraduationYear; }

    public String getAlumniName() { return alumniName; }
    public void setAlumniName(String alumniName) { this.alumniName = alumniName; }

    public String getAlumniEmail() { return alumniEmail; }
    public void setAlumniEmail(String alumniEmail) { this.alumniEmail = alumniEmail; }

    public String getAlumniDegree() { return alumniDegree; }
    public void setAlumniDegree(String alumniDegree) { this.alumniDegree = alumniDegree; }

    public String getAlumniDepartment() { return alumniDepartment; }
    public void setAlumniDepartment(String alumniDepartment) { this.alumniDepartment = alumniDepartment; }

    public String getAlumniCompany() { return alumniCompany; }
    public void setAlumniCompany(String alumniCompany) { this.alumniCompany = alumniCompany; }

    public String getAlumniRole() { return alumniRole; }
    public void setAlumniRole(String alumniRole) { this.alumniRole = alumniRole; }
}
