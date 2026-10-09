package com.alumconnect.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the schema.sql script content, syntax, and table definitions.
 */
class SchemaValidationTest {

    @Test
    @DisplayName("schema.sql should be present and contain required DDL definitions")
    void testSchemaScriptContainsRequiredTables() throws Exception {
        List<String> statements = loadSqlStatements("schema.sql");
        assertFalse(statements.isEmpty(), "Schema script should not be empty");

        String fullSql = String.join(" ", statements).toLowerCase().replaceAll("\\s+", " ");

        // Verify required tables are present
        assertTrue(fullSql.contains("create table users"), "Users table must be defined");
        assertTrue(fullSql.contains("create table student_profiles"), "Student profiles table must be defined");
        assertTrue(fullSql.contains("create table alumni_profiles"), "Alumni profiles table must be defined");
        assertTrue(fullSql.contains("create table mentorship_requests"), "Mentorship requests table must be defined");

        // Verify foreign key relationships and constraints
        assertTrue(fullSql.contains("foreign key (user_id) references users(id)"), "Profile tables must reference users(id)");
        assertTrue(fullSql.contains("foreign key (student_id) references users(id)"), "Mentorship student must reference users(id)");
        assertTrue(fullSql.contains("foreign key (alumni_id) references users(id)"), "Mentorship alumni must reference users(id)");
        assertTrue(fullSql.contains("check (student_id <> alumni_id)"), "Mentorship must disallow self-mentorship");

        // Verify mentorship status enum
        assertTrue(fullSql.contains("enum('pending', 'accepted', 'rejected', 'completed', 'cancelled')"),
                "Mentorship status must cover full workflow");

        // Verify jobs/internships/opportunities are NOT present
        assertFalse(fullSql.contains("create table jobs"), "Jobs table must not be present");
        assertFalse(fullSql.contains("create table internships"), "Internships table must not be present");
        assertFalse(fullSql.contains("create table opportunities"), "Opportunities table must not be present");
    }

    private List<String> loadSqlStatements(String resourceName) throws Exception {
        List<String> statements = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource " + resourceName + " should exist in classpath");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder current = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    current.append(line).append(" ");
                    if (trimmed.endsWith(";")) {
                        statements.add(current.toString().trim());
                        current.setLength(0);
                    }
                }
            }
        }
        return statements;
    }
}
