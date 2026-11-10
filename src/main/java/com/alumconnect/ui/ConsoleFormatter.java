package com.alumconnect.ui;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reusable utility class for consistent console UI formatting, section headers,
 * borders, dividers, and message presentation across AlumConnect (Prompt 24).
 *
 * <p>Ensures a uniform visual identity across all screens in ordinary
 * Windows and Unix terminals (50-character standard width).
 */
public final class ConsoleFormatter {

    public static final String DOUBLE_LINE = "==================================================";
    public static final String SINGLE_LINE = "--------------------------------------------------";
    public static final int TERMINAL_WIDTH = 50;

    public static final String APP_TITLE = "ALUMCONNECT";
    public static final String APP_TAGLINE = "Alumni Networking Platform";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ConsoleFormatter() {
        // Prevent instantiation of utility class
    }

    /**
     * Prints the primary AlumConnect application header with an optional section subtitle.
     *
     * <pre>
     * ==================================================
     *                    ALUMCONNECT
     *             Alumni Networking Platform
     * ==================================================
     *                 STUDENT DASHBOARD
     * --------------------------------------------------
     * </pre>
     *
     * @param out          the output print stream
     * @param sectionTitle optional section title (e.g. "STUDENT DASHBOARD")
     */
    public static void printAppHeader(PrintStream out, String sectionTitle) {
        out.println("\n" + DOUBLE_LINE);
        out.println(center(APP_TITLE, TERMINAL_WIDTH));
        out.println(center(APP_TAGLINE, TERMINAL_WIDTH));
        out.println(DOUBLE_LINE);
        if (sectionTitle != null && !sectionTitle.isBlank()) {
            out.println(center(sectionTitle.trim(), TERMINAL_WIDTH));
            out.println(SINGLE_LINE);
        }
    }

    /**
     * Prints a standalone double-bordered section header.
     *
     * <pre>
     * ==================================================
     *                 ALUMNI DIRECTORY
     * ==================================================
     * </pre>
     *
     * @param out   the output print stream
     * @param title section title text
     */
    public static void printSectionHeader(PrintStream out, String title) {
        out.println("\n" + DOUBLE_LINE);
        out.println(center(title != null ? title.trim() : "", TERMINAL_WIDTH));
        out.println(DOUBLE_LINE);
    }

    /**
     * Prints a sub-section title with dashed boundary.
     *
     * <pre>
     * --- Basic Account Details ---
     * </pre>
     *
     * @param out   the output print stream
     * @param title sub-section title
     */
    public static void printSubHeader(PrintStream out, String title) {
        out.println("\n--- " + (title != null ? title.trim() : "") + " ---");
    }

    /**
     * Prints a single 50-character horizontal divider line.
     *
     * @param out the output print stream
     */
    public static void printDivider(PrintStream out) {
        out.println(SINGLE_LINE);
    }

    /**
     * Prints a double 50-character horizontal divider line.
     *
     * @param out the output print stream
     */
    public static void printDoubleDivider(PrintStream out) {
        out.println(DOUBLE_LINE);
    }

    /**
     * Centers a string within the specified width using whitespace padding.
     * If the string length is greater than or equal to the width, it is returned as-is.
     *
     * @param text  the string to center
     * @param width terminal column width
     * @return centered string with surrounding spaces
     */
    public static String center(String text, int width) {
        if (text == null) {
            return "";
        }
        int textLen = text.length();
        if (textLen >= width) {
            return text;
        }
        int leftPadding = (width - textLen) / 2;
        int rightPadding = width - textLen - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }

    /**
     * Prints a formatted success message.
     *
     * @param out     the output print stream
     * @param message the message to print
     */
    public static void printSuccess(PrintStream out, String message) {
        out.println("[SUCCESS] " + message);
    }

    /**
     * Prints a formatted error message.
     *
     * @param out     the output print stream
     * @param message the message to print
     */
    public static void printError(PrintStream out, String message) {
        out.println("[ERROR] " + message);
    }

    /**
     * Prints a formatted informational message.
     *
     * @param out     the output print stream
     * @param message the message to print
     */
    public static void printInfo(PrintStream out, String message) {
        out.println("[INFO] " + message);
    }

    /**
     * Converts null or blank strings into a readable fallback string ("Not Specified").
     *
     * @param value raw value
     * @return trimmed non-blank string or "Not Specified"
     */
    public static String safeString(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "Not Specified";
    }

    /**
     * Formats a {@link LocalDateTime} using {@code yyyy-MM-dd HH:mm:ss} format,
     * or returns "Not Specified" if null.
     *
     * @param ldt the timestamp
     * @return formatted date string
     */
    public static String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return "Not Specified";
        }
        return ldt.format(DATE_TIME_FORMATTER);
    }
}

