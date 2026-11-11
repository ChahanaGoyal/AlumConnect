package com.alumconnect.web;

import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.util.ConnectionPool;
import com.alumconnect.util.DatabaseInitializer;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight web server providing a browser-based frontend for AlumConnect.
 * Uses JDK 17's built-in {@link HttpServer} without external frameworks or dependencies.
 */
public class AlumConnectWebServer {

    private static final Logger LOGGER = Logger.getLogger(AlumConnectWebServer.class.getName());
    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private HttpServer server;
    private ThreadPoolExecutor threadPoolExecutor;

    private final UserService userService;
    private final StudentProfileService studentProfileService;
    private final AlumniProfileService alumniProfileService;
    private final AlumniDirectoryService alumniDirectoryService;
    private final MentorshipRequestService mentorshipRequestService;

    /**
     * Creates an AlumConnectWebServer instance on the default port (8080)
     * using standard backend business services.
     */
    public AlumConnectWebServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Creates an AlumConnectWebServer instance on the specified port
     * using standard backend business services.
     *
     * @param port the HTTP port to listen on
     */
    public AlumConnectWebServer(int port) {
        this(port,
             new UserService(),
             new StudentProfileService(),
             new AlumniProfileService(),
             new AlumniDirectoryService(),
             new MentorshipRequestService());
    }

    /**
     * Dependency injection constructor (useful for isolated unit testing).
     */
    public AlumConnectWebServer(int port,
                                UserService userService,
                                StudentProfileService studentProfileService,
                                AlumniProfileService alumniProfileService,
                                AlumniDirectoryService alumniDirectoryService,
                                MentorshipRequestService mentorshipRequestService) {
        this.port = port;
        this.userService = userService;
        this.studentProfileService = studentProfileService;
        this.alumniProfileService = alumniProfileService;
        this.alumniDirectoryService = alumniDirectoryService;
        this.mentorshipRequestService = mentorshipRequestService;
    }

    /**
     * Initializes and starts the HTTP server.
     *
     * @throws IOException if server fails to bind or start
     */
    public synchronized void start() throws IOException {
        if (server != null) {
            LOGGER.warning("Server is already running.");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(threadPoolExecutor);

        // Register application endpoints
        server.createContext("/", new WebHandlers.HomeHandler());
        server.createContext("/login", new WebHandlers.LoginHandler(userService));
        server.createContext("/register", new WebHandlers.RegisterHandler(userService));
        server.createContext("/logout", new WebHandlers.LogoutHandler(userService));
        server.createContext("/student/dashboard", new WebHandlers.StudentDashboardHandler(
                studentProfileService, alumniDirectoryService, mentorshipRequestService));
        server.createContext("/student/send-request", new WebHandlers.StudentSendRequestHandler(
                mentorshipRequestService));
        server.createContext("/alumni/dashboard", new WebHandlers.AlumniDashboardHandler(
                alumniProfileService, mentorshipRequestService));
        server.createContext("/alumni/request-action", new WebHandlers.AlumniActionHandler(
                mentorshipRequestService));

        server.start();
        LOGGER.info("AlumConnect Web Server started on port " + getPort());
    }

    /**
     * Stops the HTTP server gracefully.
     *
     * @param delaySeconds seconds to wait for active requests to finish
     */
    public synchronized void stop(int delaySeconds) {
        if (server != null) {
            server.stop(delaySeconds);
            server = null;
        }
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
            threadPoolExecutor = null;
        }
        LOGGER.info("AlumConnect Web Server stopped.");
    }

    /**
     * Returns the actual bound port (useful when port 0 is used for ephemeral testing).
     *
     * @return bound port number
     */
    public int getPort() {
        if (server != null) {
            return server.getAddress().getPort();
        }
        return port;
    }

    /**
     * Main entry point to run the web server standalone.
     *
     * @param args optional command line arguments (e.g. port)
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0] + ", using default " + DEFAULT_PORT);
            }
        }

        System.out.println("==================================================");
        System.out.println("   ALUMCONNECT BROWSER WEB SERVER INITIALIZING    ");
        System.out.println("==================================================");

        try {
            System.out.println("[Web Server] Initializing database schema...");
            DatabaseInitializer.initializeDatabase();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database initialization warning: " + e.getMessage());
        }

        AlumConnectWebServer webServer = new AlumConnectWebServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Web Server] Shutting down AlumConnect Web Server...");
            webServer.stop(1);
            ConnectionPool.getInstance().shutdown();
            System.out.println("[Web Server] Shutdown complete.");
        }));

        try {
            webServer.start();
            String rootUrl = "http://localhost:" + webServer.getPort() + "/";
            System.out.println("\nAlumConnect Web Interface is ready!");
            System.out.println("Access URL : " + rootUrl);
            System.out.println("Login URL  : http://localhost:" + webServer.getPort() + "/login");
            System.out.println("Register   : http://localhost:" + webServer.getPort() + "/register");
            System.out.println("\nPress Ctrl+C to terminate the server.\n");

            // Automatically open Google Chrome / browser once server is running
            openBrowser(rootUrl);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start AlumConnect Web Server", e);
            System.exit(1);
        }
    }

    /**
     * Attempts to open the target URL in Google Chrome (or default browser as fallback).
     * Captures and reports diagnostics for any failed launch step without interrupting
     * server operation.
     *
     * @param url the web URL to launch
     * @return true if a browser process was launched; false otherwise
     */
    public static boolean openBrowser(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        boolean launched = false;
        List<String> diagnostics = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();

        try {
            // ── Strategy 1: Direct Chrome executable on Windows ─────────────────
            if (os.contains("win")) {
                File chromeExe = findChromeExecutable(diagnostics);
                if (chromeExe != null) {
                    try {
                        Process process = new ProcessBuilder(chromeExe.getAbsolutePath(), url).start();
                        launched = true;
                        System.out.println("[Web Server] Google Chrome launched via: " + chromeExe.getAbsolutePath());
                    } catch (Exception e) {
                        diagnostics.add("ProcessBuilder failed on " + chromeExe.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }

            // ── Strategy 2: Windows Start Menu Shortcut ─────────────────────────
            if (!launched && os.contains("win")) {
                File shortcut = findChromeShortcut(diagnostics);
                if (shortcut != null) {
                    try {
                        Process process = new ProcessBuilder("cmd.exe", "/c", "start", "", shortcut.getAbsolutePath(), url).start();
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            launched = true;
                            System.out.println("[Web Server] Google Chrome launched via shortcut: " + shortcut.getAbsolutePath());
                        } else {
                            diagnostics.add("cmd.exe start shortcut exited with code " + exitCode);
                        }
                    } catch (Exception e) {
                        diagnostics.add("cmd.exe start shortcut failed: " + e.getMessage());
                    }
                }
            }

            // ── Strategy 3: cmd.exe start "" chrome <url> ───────────────────────
            if (!launched && os.contains("win")) {
                try {
                    Process process = new ProcessBuilder("cmd.exe", "/c", "start", "", "chrome", url).start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        launched = true;
                        System.out.println("[Web Server] Google Chrome launched via 'cmd.exe start chrome'");
                    } else {
                        diagnostics.add("'cmd.exe start chrome' exited with code " + exitCode);
                    }
                } catch (Exception e) {
                    diagnostics.add("'cmd.exe start chrome' failed: " + e.getMessage());
                }
            }

            // ── Strategy 4: Java Desktop API ────────────────────────────────────
            if (!launched) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() &&
                            java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(new URI(url));
                        launched = true;
                        System.out.println("[Web Server] Browser opened via Java Desktop API.");
                    } else {
                        diagnostics.add("Java Desktop BROWSE action is not supported in this environment");
                    }
                } catch (Exception e) {
                    diagnostics.add("Java Desktop API browse failed: " + e.getMessage());
                }
            }

            // ── Strategy 5: OS-specific generic shell open ──────────────────────
            if (!launched) {
                try {
                    Process process = null;
                    if (os.contains("win")) {
                        // Notice the empty title "" so cmd.exe start does not treat url as title
                        process = new ProcessBuilder("cmd.exe", "/c", "start", "", url).start();
                    } else if (os.contains("mac")) {
                        process = new ProcessBuilder("open", url).start();
                    } else if (os.contains("nix") || os.contains("nux")) {
                        process = new ProcessBuilder("xdg-open", url).start();
                    }

                    if (process != null) {
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            launched = true;
                            System.out.println("[Web Server] Default system browser launched.");
                        } else {
                            diagnostics.add("Default shell open exited with code " + exitCode);
                        }
                    }
                } catch (Exception e) {
                    diagnostics.add("Default shell open failed: " + e.getMessage());
                }
            }
        } catch (Throwable t) {
            diagnostics.add("Unexpected error in openBrowser: " + t.getMessage());
        }

        if (launched) {
            System.out.println("[Web Server] Browser session active: " + url);
        } else {
            System.out.println("[Web Server] Notice: Could not automatically launch Google Chrome.");
            if (!diagnostics.isEmpty()) {
                System.out.println("[Web Server] Diagnostics:");
                for (String diag : diagnostics) {
                    System.out.println("  - " + diag);
                }
            }
            System.out.println("[Web Server] Please open your browser manually at:");
            System.out.println("             " + url);
        }

        return launched;
    }

    private static File findChromeExecutable(List<String> diagnostics) {
        List<String> candidates = new ArrayList<>();

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            candidates.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            candidates.add(userHome + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe");
            candidates.add(userHome + "\\AppData\\Local\\Programs\\Google\\Chrome\\Application\\chrome.exe");
        }
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            candidates.add(userProfile + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe");
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null && !programFiles.isBlank()) {
            candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null && !programFilesX86.isBlank()) {
            candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        candidates.add("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        candidates.add("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");

        for (String path : candidates) {
            try {
                File f = new File(path);
                if (f.isFile()) {
                    return f;
                }
            } catch (Exception e) {
                diagnostics.add("Error checking candidate path " + path + ": " + e.getMessage());
            }
        }

        // Try reading registered Chrome path from Windows Registry
        String[] regKeys = new String[]{
                "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe",
                "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe"
        };

        for (String key : regKeys) {
            try {
                Process proc = new ProcessBuilder("reg.exe", "query", key, "/ve").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("REG_SZ")) {
                            int idx = line.indexOf("REG_SZ");
                            String regPath = line.substring(idx + "REG_SZ".length()).trim();
                            if (regPath.startsWith("\"") && regPath.endsWith("\"") && regPath.length() > 2) {
                                regPath = regPath.substring(1, regPath.length() - 1);
                            }
                            File regFile = new File(regPath);
                            if (regFile.isFile()) {
                                return regFile;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Registry query may fail if key does not exist or access is restricted
            }
        }

        return null;
    }

    private static File findChromeShortcut(List<String> diagnostics) {
        List<String> shortcutCandidates = new ArrayList<>();
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            shortcutCandidates.add(appData + "\\Microsoft\\Windows\\Start Menu\\Programs\\Google Chrome.lnk");
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            shortcutCandidates.add(userHome + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Google Chrome.lnk");
            shortcutCandidates.add(userHome + "\\Desktop\\Google Chrome.lnk");
            shortcutCandidates.add(userHome + "\\OneDrive\\Desktop\\Google Chrome.lnk");
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && !programData.isBlank()) {
            shortcutCandidates.add(programData + "\\Microsoft\\Windows\\Start Menu\\Programs\\Google Chrome.lnk");
        }
        shortcutCandidates.add("C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\Google Chrome.lnk");
        shortcutCandidates.add("C:\\Users\\Public\\Desktop\\Google Chrome.lnk");

        for (String path : shortcutCandidates) {
            try {
                File f = new File(path);
                if (f.isFile()) {
                    return f;
                }
            } catch (Exception e) {
                diagnostics.add("Error checking shortcut path " + path + ": " + e.getMessage());
            }
        }
        return null;
    }
}

