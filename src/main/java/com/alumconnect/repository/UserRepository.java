package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Data Access interface for user authentication and role-specific profile persistence.
 */
public interface UserRepository {

    /**
     * Checks if a user already exists with the given email address.
     *
     * @param email email address to check (case-insensitive)
     * @return {@code true} if a user record exists; {@code false} otherwise
     * @throws SQLException if a database error occurs
     */
    boolean existsByEmail(String email) throws SQLException;

    /**
     * Finds a basic user record by primary key ID.
     *
     * @param id user primary key
     * @return Optional containing the user if found
     * @throws SQLException if a database error occurs
     */
    Optional<User> findById(int id) throws SQLException;

    /**
     * Finds a basic user record by email.
     *
     * @param email user email address
     * @return Optional containing the user if found
     * @throws SQLException if a database error occurs
     */
    Optional<User> findByEmail(String email) throws SQLException;

    /**
     * Finds a complete Student entity (users + student_profiles) by user ID.
     *
     * @param userId user primary key ID
     * @return Optional containing the Student if found
     * @throws SQLException if a database error occurs
     */
    Optional<Student> findStudentById(int userId) throws SQLException;

    /**
     * Finds a complete Alumni entity (users + alumni_profiles) by user ID.
     *
     * @param userId user primary key ID
     * @return Optional containing the Alumni if found
     * @throws SQLException if a database error occurs
     */
    Optional<Alumni> findAlumniById(int userId) throws SQLException;

    /**
     * Persists a new Student entity in a single atomic transaction:
     * inserts into {@code users}, captures the generated user id,
     * then inserts into {@code student_profiles}.
     *
     * @param student the Student model to persist
     * @return the persisted Student with generated IDs assigned
     * @throws SQLException if an insertion or constraint violation occurs
     */
    Student saveStudent(Student student) throws SQLException;

    /**
     * Persists a new Alumni entity in a single atomic transaction:
     * inserts into {@code users}, captures the generated user id,
     * then inserts into {@code alumni_profiles}.
     *
     * @param alumni the Alumni model to persist
     * @return the persisted Alumni with generated IDs assigned
     * @throws SQLException if an insertion or constraint violation occurs
     */
    Alumni saveAlumni(Alumni alumni) throws SQLException;

    /**
     * Deletes a user by ID. Cascades to student_profiles or alumni_profiles via foreign key.
     *
     * @param userId user ID to delete
     * @return {@code true} if a record was deleted
     * @throws SQLException if a database error occurs
     */
    boolean deleteUserById(int userId) throws SQLException;
}
