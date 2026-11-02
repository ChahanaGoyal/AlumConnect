package com.alumconnect.service.dto;

/**
 * Data Transfer Object representing a mentorship request creation payload.
 */
public class MentorshipRequestDto {
    private int studentId;
    private int alumniId;
    private String topic;
    private String message;

    public MentorshipRequestDto() {}

    public MentorshipRequestDto(int studentId, int alumniId, String topic, String message) {
        this.studentId = studentId;
        this.alumniId = alumniId;
        this.topic = topic;
        this.message = message;
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
}

