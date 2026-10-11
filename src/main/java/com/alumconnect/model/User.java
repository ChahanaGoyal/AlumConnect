package com.alumconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a registered user of the AlumConnect platform.
 *
 * <p>Maps to the {@code users} table, which is the single authentication and
 * identity record for every platform member regardless of role. Concrete
 * subclasses ({@link Student}, {@link Alumni}) extend this class to add their
 * role-specific profile fields from the {@code student_profiles} and
 * {@code alumni_profiles} tables respectively.
 *
 * <p>Field-to-column mapping:
 * <pre>
 *  id           ← users.id           (INT AUTO_INCREMENT PK)
 *  email        ← users.email        (VARCHAR 150, UNIQUE)
 *  passwordHash ← users.password_hash (VARCHAR 255)
 *  fullName     ← users.full_name    (VARCHAR 100)
 *  role         ← users.role         (ENUM)
 *  phone        ← users.phone        (VARCHAR 20, nullable)
 *  active       ← users.is_active    (BOOLEAN DEFAULT TRUE)
 *  createdAt    ← users.created_at   (TIMESTAMP)
 *  updatedAt    ← users.updated_at   (TIMESTAMP)
 * </pre>
 *
 * <p>Design decisions:
 * <ul>
 *   <li>The class is {@code abstract} because a bare {@code User} with no profile
 *       cannot exist on the platform — every user is either a Student, Alumni, or Admin.</li>
 *   <li>Password storage: only a pre-computed hash is held here; raw passwords
 *       are never stored in model objects.</li>
 *   <li>Timestamps are {@link LocalDateTime} (UTC) to remain database-agnostic.</li>
 * </ul>
 */
public abstract class User {

    // ── Fields (mapped to users table columns) ────────────────────────────────

    /** Primary key — {@code users.id}. Null until persisted. */
    private Integer id;

    /** Unique email address — {@code users.email}. */
    private String email;

    /**
     * Bcrypt / SHA-256 hash of the user's password — {@code users.password_hash}.
     * Never stores a raw plaintext password.
     */
    private String passwordHash;

    /** Full display name — {@code users.full_name}. */
    private String fullName;

    /** Platform role — {@code users.role}. */
    private Role role;

    /** Optional contact phone — {@code users.phone}. May be {@code null}. */
    private String phone;

    /** Whether the account is active — {@code users.is_active}. */
    private boolean active;

    /** Record creation timestamp — {@code users.created_at}. */
    private LocalDateTime createdAt;

    /** Record last-updated timestamp — {@code users.updated_at}. */
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * No-arg constructor for framework / JDBC row-mapping use.
     * Defaults {@code active} to {@code true}.
     */
    protected User() {
        this.active = true;
    }

    /**
     * Full constructor for creating a new user record before persistence.
     * {@code id}, {@code createdAt}, and {@code updatedAt} are set by the database
     * and are left null here.
     *
     * @param email        unique, valid email address
     * @param passwordHash pre-computed hash — never a raw password
     * @param fullName     display name
     * @param role         platform role
     * @param phone        optional phone number (may be {@code null})
     */
    protected User(String email, String passwordHash, String fullName,
                   Role role, String phone) {
        this();
        this.email        = Objects.requireNonNull(email,        "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.fullName     = Objects.requireNonNull(fullName,     "fullName must not be null");
        this.role         = Objects.requireNonNull(role,         "role must not be null");
        this.phone        = phone;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return the database primary key, or {@code null} if not yet persisted */
    public Integer getId() { return id; }

    /** @return the user's unique email address */
    public String getEmail() { return email; }

    /** @return the stored password hash (never the raw password) */
    public String getPasswordHash() { return passwordHash; }

    /** @return the user's full display name */
    public String getFullName() { return fullName; }

    /** @return the platform {@link Role} assigned to this user */
    public Role getRole() { return role; }

    /** @return the optional phone number, or {@code null} */
    public String getPhone() { return phone; }

    /** @return {@code true} if the account is active */
    public boolean isActive() { return active; }

    /** @return the UTC timestamp when this record was created, or {@code null} before first save */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @return the UTC timestamp of the last update, or {@code null} before first save */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** Sets the database-assigned primary key. Called by the repository after INSERT. */
    public void setId(Integer id) { this.id = id; }

    /**
     * Updates the email address.
     *
     * @param email a valid, non-null email address
     */
    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "email must not be null");
    }

    /**
     * Updates the stored password hash.
     *
     * @param passwordHash a pre-computed hash — never a raw password
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    /**
     * Updates the full display name.
     *
     * @param fullName non-null display name
     */
    public void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
    }

    /**
     * Updates the platform role. Changing a user's role after creation is an
     * administrative action — use with care.
     *
     * @param role non-null role
     */
    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    /** @param phone optional phone number; {@code null} is permitted */
    public void setPhone(String phone) { this.phone = phone; }

    /** @param active {@code false} to soft-delete / deactivate this account */
    public void setActive(boolean active) { this.active = active; }

    /** @param createdAt set by the repository when mapping a database row */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @param updatedAt set by the repository when mapping a database row */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this user's role is {@link Role#STUDENT}.
     *
     * @return {@code true} for student accounts
     */
    public boolean isStudent() {
        return Role.STUDENT == this.role;
    }

    /**
     * Returns {@code true} if this user's role is {@link Role#ALUMNI}.
     *
     * @return {@code true} for alumni accounts
     */
    public boolean isAlumni() {
        return Role.ALUMNI == this.role;
    }

    /**
     * Returns {@code true} if this user's role is {@link Role#ADMIN}.
     *
     * @return {@code true} for admin accounts
     */
    public boolean isAdmin() {
        return Role.ADMIN == this.role;
    }

    /**
     * Returns a human-readable display label combining name and role,
     * suitable for logging and console output.
     *
     * @return e.g. {@code "Jane Doe [STUDENT]"}
     */
    public String getDisplayLabel() {
        return fullName + " [" + (role != null ? role.name() : "UNKNOWN") + "]";
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    /**
     * Two users are equal when they share the same non-null database {@code id}.
     * If both have null ids they are considered not equal (unpersisted objects).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a safe string representation that never exposes the password hash.
     *
     * @return e.g. {@code "User{id=1, email='jane@example.com', role=STUDENT, active=true}"}
     */
    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", email='" + email + '\'' +
               ", fullName='" + fullName + '\'' +
               ", role=" + role +
               ", active=" + active +
               '}';
    }
}
