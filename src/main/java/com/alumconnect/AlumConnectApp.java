package com.alumconnect;

import com.alumconnect.ui.AuthUI;
import com.alumconnect.ui.ConsoleFormatter;
import com.alumconnect.ui.RegistrationUI;
import com.alumconnect.util.ConnectionPool;
import com.alumconnect.util.DatabaseConnection;
import com.alumconnect.util.DatabaseHealthCheck;
import com.alumconnect.util.DatabaseInitializer;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Main entry point for the AlumConnect Application.
 * AlumConnect is an Alumni Networking &amp; Mentorship Platform.
 */
public class AlumConnectApp {

    public static final String APP_NAME = "AlumConnect";
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        ConsoleFormatter.printAppHeader(System.out, "Welcome to " + APP_NAME + " (v" + VERSION + ")");
        System.out.println("Project foundation initialized successfully.");

        // ── Database Initialization ──────────────────────────────────────────
        System.out.println("\n[AlumConnect] Setting up database...");
        DatabaseInitializer.initializeDatabase();

        // ── JDBC Connection Health Check ─────────────────────────────────────
        System.out.println("[AlumConnect] Verifying JDBC connection...");
        try (Connection conn = ConnectionPool.getInstance().acquire()) {
            boolean ok = DatabaseHealthCheck.verifyConnection(conn);
            if (ok) {
                System.out.println("[AlumConnect] JDBC connection verified successfully.");
            } else {
                System.err.println("[AlumConnect] Database schema verification failed. Check the schema.");
            }
        } catch (SQLException e) {
            System.err.println("[AlumConnect] Could not obtain a database connection: " + e.getMessage());
            System.err.println("[AlumConnect] Application will continue without database connectivity.");
        }

        System.out.println("[AlumConnect] Startup complete.");

        // ── Interactive Console Menu ─────────────────────────────────────────
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            AuthUI authUI = new AuthUI();
            RegistrationUI registrationUI = new RegistrationUI();
            runMainMenu(scanner, System.out, authUI, registrationUI);
        }

        // Graceful shutdown of pool resources on exit
        ConnectionPool.getInstance().shutdown();
    }

    /**
     * Interactive Main Menu loop for AlumConnect (Prompt 24).
     *
     * @param scanner        the console scanner
     * @param out            the output print stream
     * @param authUI         the authentication UI
     * @param registrationUI the user registration UI
     */
    public static void runMainMenu(Scanner scanner, PrintStream out, AuthUI authUI, RegistrationUI registrationUI) {
        while (true) {
            ConsoleFormatter.printAppHeader(out, "MAIN MENU");
            out.println("Welcome to " + APP_NAME + " (v" + VERSION + ")!");
            ConsoleFormatter.printDivider(out);
            out.println("Main Options:");
            out.println("  [1] Login (Student or Alumni)");
            out.println("  [2] Register New Account");
            out.println("  [0] Exit");
            ConsoleFormatter.printDivider(out);
            out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> authUI.promptAndLogin();
                case "2" -> registrationUI.promptAndRegister();
                case "0", "EXIT", "Q", "QUIT" -> {
                    out.println("\n[AlumConnect] Thank you for using AlumConnect. Goodbye!\n");
                    return;
                }
                default -> out.println("[AlumConnect] Invalid choice '" + choice + "'. Please enter 1, 2, or 0.");
            }
        }
    }
}
