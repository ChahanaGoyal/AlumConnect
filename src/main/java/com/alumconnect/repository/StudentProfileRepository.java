package com.alumconnect.repository;

import com.alumconnect.model.Student;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Data Access interface for student profile persistence operations.
 *
 * <p>Handles reading and updating the {@code student_profiles} table for
 * an already-authenticated student. Profile creation is handled at
 * registration time by {@link UserRepository#saveStudent(Student)}.
 */
public interface StudentProfileRepository {

    /**
     * Finds the full student profile (users + student_profiles JOIN) by the user's primary key.
     *
     * @param userId the {@code users.id} PK of the logged-in student
     * @return Optional containing the Student if a matching profile row exists
     * @throws SQLException if a database error occurs
     */
    Optional<Student> findProfileByUserId(int userId) throws SQLException;

    /**
     * Updates mutable profile fields in the {@code student_profiles} table for the given user.
     *
     * <p>Only the fields settable by the student after registration are updated here:
     * enrollment_number, degree, department, graduation_year, current_semester,
     * bio, skills, interests, linkedin_url, github_url.
     *
     * @param student the Student object containing updated profile data; {@code getId()} is used
     *                as the WHERE clause key (user_id)
     * @return {@code true} if at least one row was updated; {@code false} if no matching profile found
     * @throws SQLException if a database error or constraint violation occurs
     */
    boolean updateProfile(Student student) throws SQLException;

    /**
     * Creates a new student_profiles row for the given student.
     *
     * @param student the Student model containing the profile details; {@code getId()} is used
     *                as the foreign key (user_id)
     * @return {@code true} if inserted successfully
     * @throws SQLException if a database error occurs
     */
    boolean createProfile(Student student) throws SQLException;

    /**
     * Checks whether a {@code student_profiles} row already exists for the given user.
     *
     * @param userId the {@code users.id} PK
     * @return {@code true} if a profile row exists
     * @throws SQLException if a database error occurs
     */
    boolean profileExistsForUser(int userId) throws SQLException;
}
