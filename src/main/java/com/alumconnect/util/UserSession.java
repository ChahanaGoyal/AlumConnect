package com.alumconnect.util;

import com.alumconnect.model.Role;
import com.alumconnect.model.User;

import java.util.Optional;

/**
 * Manages the current logged-in user session state.
 * Thread-safe singleton providing login state tracking, role checking,
 * access control enforcement, and logout functionality.
 */
public class UserSession {

    private static final UserSession INSTANCE = new UserSession();

    private volatile User currentUser;

    private UserSession() {
        this.currentUser = null;
    }

    /**
     * Returns the singleton instance of UserSession.
     *
     * @return UserSession instance
     */
    public static UserSession getInstance() {
        return INSTANCE;
    }

    /**
     * Establishes a session for the specified authenticated user.
     *
     * @param user the authenticated User
     * @throws IllegalArgumentException if user is null
     */
    public synchronized void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot login with null user.");
        }
        this.currentUser = user;
    }

    /**
     * Clears the current session and logs out the user.
     */
    public synchronized void logout() {
        this.currentUser = null;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return {@code true} if a user is logged in; {@code false} otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns an Optional containing the current authenticated User, or empty if logged out.
     *
     * @return Optional of currentUser
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Returns the role of the currently logged-in user.
     *
     * @return Optional containing the Role, or empty if not logged in
     */
    public Optional<Role> getCurrentRole() {
        return Optional.ofNullable(currentUser).map(User::getRole);
    }

    /**
     * Checks if the currently logged-in user is a Student.
     *
     * @return {@code true} if logged in and role is STUDENT; {@code false} otherwise
     */
    public boolean isStudent() {
        return currentUser != null && currentUser.isStudent();
    }

    /**
     * Checks if the currently logged-in user is an Alumni.
     *
     * @return {@code true} if logged in and role is ALUMNI; {@code false} otherwise
     */
    public boolean isAlumni() {
        return currentUser != null && currentUser.isAlumni();
    }

    /**
     * Checks if the currently logged-in user has permission for the specified role.
     *
     * @param requiredRole the role required for an operation
     * @return {@code true} if the current user has the required role; {@code false} otherwise
     */
    public boolean hasRole(Role requiredRole) {
        if (currentUser == null || requiredRole == null) {
            return false;
        }
        return currentUser.getRole() == requiredRole;
    }

    /**
     * Enforces access control by verifying that the logged-in user has the required role.
     *
     * @param requiredRole the role required
     * @throws SecurityException if not logged in or role does not match
     */
    public void requireRole(Role requiredRole) {
        if (requiredRole == null) {
            throw new IllegalArgumentException("Required role must not be null.");
        }
        if (!isLoggedIn()) {
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        if (!hasRole(requiredRole)) {
            throw new SecurityException("Access Denied: Current user role [" +
                    currentUser.getRole() + "] cannot access " + requiredRole + "-only functionality.");
        }
    }

    /**
     * Enforces access control by verifying that the current logged-in user is a Student.
     *
     * @throws SecurityException if not logged in or role is not STUDENT
     */
    public void requireStudent() {
        requireRole(Role.STUDENT);
    }

    /**
     * Enforces access control by verifying that the current logged-in user is an Alumni.
     *
     * @throws SecurityException if not logged in or role is not ALUMNI
     */
    public void requireAlumni() {
        requireRole(Role.ALUMNI);
    }

    /**
     * Enforces resource ownership by verifying that the current user's ID matches expectedUserId.
     *
     * <p>Must only be called after authentication has already been verified (e.g. via
     * {@link #requireStudent()} or {@link #requireAlumni()}).
     *
     * @param expectedUserId the resource owner's user ID
     * @throws SecurityException if not logged in or the current user ID does not match
     */
    public void requireUserId(int expectedUserId) {
        if (!isLoggedIn()) {
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        User user = currentUser;
        if (user.getId() == null || user.getId() != expectedUserId) {
            throw new SecurityException(
                    "Access Denied: You cannot access or modify resources belonging to another user.");
        }
    }

    /**
     * Returns the authenticated user's ID.
     *
     * @return the logged-in user's ID (always positive)
     * @throws SecurityException if not logged in or the user has an invalid (null/non-positive) ID
     */
    public int getAuthenticatedUserId() {
        if (!isLoggedIn()) {
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        User user = currentUser;
        if (user.getId() == null || user.getId() <= 0) {
            throw new SecurityException("Unauthorized: Invalid user session ID.");
        }
        return user.getId();
    }
}
