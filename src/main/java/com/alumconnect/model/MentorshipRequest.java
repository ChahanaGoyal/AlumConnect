package com.alumconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing a row in the {@code mentorship_requests} table.
 * Mirrors the database schema defined in schema.sql.
 */
public class MentorshipRequest {
    private Integer id; // AUTO_INCREMENT primary key
    private int studentId; // FK to users.id (student)
    private int alumniId; // FK to users.id (alumni)
    private String topic;
    private String message;
    private MentorshipStatus status = MentorshipStatus.PENDING;
    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

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

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

