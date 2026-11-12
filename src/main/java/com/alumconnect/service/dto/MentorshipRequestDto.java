package com.alumconnect.service.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data Transfer Object representing a mentorship request creation payload.
 */
public class MentorshipRequestDto {
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private int studentId;
    private int alumniId;
    private String topic;
    private String message;
    private LocalDateTime requestedAt;

    public MentorshipRequestDto() {
        this.requestedAt = LocalDateTime.now(DEFAULT_ZONE);
    }

    public MentorshipRequestDto(int studentId, int alumniId, String topic, String message) {
        this(studentId, alumniId, topic, message, LocalDateTime.now(DEFAULT_ZONE));
    }

    public MentorshipRequestDto(int studentId, int alumniId, String topic, String message, LocalDateTime requestedAt) {
        this.studentId = studentId;
        this.alumniId = alumniId;
        this.topic = topic;
        this.message = message;
        this.requestedAt = requestedAt != null ? requestedAt : LocalDateTime.now(DEFAULT_ZONE);
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getAlumniId() {
        return alumniId;
    }

    public void setAlumniId(int alumniId) {
        this.alumniId = alumniId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}

