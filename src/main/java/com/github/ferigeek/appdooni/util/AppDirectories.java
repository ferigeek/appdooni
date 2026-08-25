package com.github.ferigeek.appdooni.util;

import java.nio.file.Path;

/**
 * Resolves the standard application data directory, database and log paths for
 * the current operating system, following each platform's convention.
 */
public final class AppDirectories {

    private AppDirectories() {
    }

    /**
     * Returns the application data directory for the current operating system:
     * <ul>
     * <li>Linux: {@code $XDG_DATA_HOME/appdooni} or {@code ~/.local/share/appdooni}</li>
     * <li>Windows: {@code %LOCALAPPDATA%\appdooni}</li>
     * <li>macOS: {@code ~/Library/Application Support/appdooni}</li>
     * </ul>
     */
    public static Path getDataDirectory() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        Path base;
        if (osName.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            base = localAppData != null && !localAppData.isBlank()
                    ? Path.of(localAppData)
                    : Path.of(System.getProperty("user.home"), "AppData", "Local");
        } else if (osName.contains("mac")) {
            base = Path.of(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            base = xdgDataHome != null && !xdgDataHome.isBlank()
                    ? Path.of(xdgDataHome)
                    : Path.of(System.getProperty("user.home"), ".local", "share");
        }
        return base.resolve("appdooni");
    }

    /** Returns the path to the application database file. */
    public static Path getDatabasePath() {
        return getDataDirectory().resolve("appdooni.db");
    }

    /** Returns the path to the application log file, stored next to the database. */
    public static Path getLogPath() {
        return getDataDirectory().resolve("appdooni.log");
    }
}