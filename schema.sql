-- =====================================================================
-- AlumConnect Database Schema
-- Alumni Networking & Mentorship Platform
-- =====================================================================

-- 1. Create Database if not exists
CREATE DATABASE IF NOT EXISTS alumconnect_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE alumconnect_db;

-- 2. Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS mentorship_requests;
DROP TABLE IF EXISTS alumni_profiles;
DROP TABLE IF EXISTS student_profiles;
DROP TABLE IF EXISTS users;

-- =====================================================================
-- Table: users
-- Core authentication and identity table for all platform members
-- =====================================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('STUDENT', 'ALUMNI', 'ADMIN') NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_email_format CHECK (email LIKE '%_@__%.__%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);

-- =====================================================================
-- Table: student_profiles
-- Profile details specific to enrolled students
-- =====================================================================
CREATE TABLE student_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    enrollment_number VARCHAR(50) UNIQUE DEFAULT NULL,
    degree VARCHAR(100) NOT NULL DEFAULT 'Bachelor of Technology',
    department VARCHAR(100) NOT NULL,
    graduation_year INT NOT NULL,
    current_semester INT DEFAULT 1,
    bio TEXT DEFAULT NULL,
    skills TEXT DEFAULT NULL,
    interests TEXT DEFAULT NULL,
    linkedin_url VARCHAR(255) DEFAULT NULL,
    github_url VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_student_semester CHECK (current_semester >= 1 AND current_semester <= 12),
    CONSTRAINT chk_student_grad_year CHECK (graduation_year >= 1950 AND graduation_year <= 2100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_student_department ON student_profiles(department);
CREATE INDEX idx_student_grad_year ON student_profiles(graduation_year);

-- =====================================================================
-- Table: alumni_profiles
-- Profile details specific to graduated alumni & mentors
-- =====================================================================
CREATE TABLE alumni_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    graduation_year INT NOT NULL,
    degree VARCHAR(100) NOT NULL DEFAULT 'Bachelor of Technology',
    department VARCHAR(100) NOT NULL,
    current_company VARCHAR(150) DEFAULT NULL,
    current_role VARCHAR(100) DEFAULT NULL,
    industry VARCHAR(100) DEFAULT NULL,
    years_of_experience INT DEFAULT 0,
    expertise_areas TEXT DEFAULT NULL,
    bio TEXT DEFAULT NULL,
    linkedin_url VARCHAR(255) DEFAULT NULL,
    is_available_for_mentorship BOOLEAN NOT NULL DEFAULT TRUE,
    max_mentees INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_alumni_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_alumni_experience CHECK (years_of_experience >= 0),
    CONSTRAINT chk_alumni_max_mentees CHECK (max_mentees >= 0),
    CONSTRAINT chk_alumni_grad_year CHECK (graduation_year >= 1950 AND graduation_year <= 2100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alumni_department ON alumni_profiles(department);
CREATE INDEX idx_alumni_company ON alumni_profiles(current_company);
CREATE INDEX idx_alumni_industry ON alumni_profiles(industry);
CREATE INDEX idx_alumni_mentorship ON alumni_profiles(is_available_for_mentorship);

-- =====================================================================
-- Table: mentorship_requests
-- Handles student-to-alumni mentorship lifecycle
-- =====================================================================
CREATE TABLE mentorship_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    alumni_id INT NOT NULL,
    topic VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    response_notes TEXT DEFAULT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL DEFAULT NULL,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentorship_student FOREIGN KEY (student_id) 
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mentorship_alumni FOREIGN KEY (alumni_id) 
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_distinct_participants CHECK (student_id <> alumni_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentorship_student ON mentorship_requests(student_id);
CREATE INDEX idx_mentorship_alumni ON mentorship_requests(alumni_id);
CREATE INDEX idx_mentorship_status ON mentorship_requests(status);
