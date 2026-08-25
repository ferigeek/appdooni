package com.github.ferigeek.appdooni.repository;

import com.github.ferigeek.appdooni.util.AppDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the SQLite database connection and initializes the schema on first use.
 * All SQLite access in the application goes through connections from here.
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String JDBC_PREFIX = "jdbc:sqlite:";

    private final String url;
    private final Path databasePath;

    /** Creates a manager for the standard application database. */
    public DatabaseManager() {
        this(AppDirectories.getDatabasePath());
    }

    /** Creates a manager for a database at the given path. */
    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
        this.url = JDBC_PREFIX + databasePath;
        try {
            initialize();
        } catch (IOException | SQLException e) {
            log.error("Failed to initialize database at {}", databasePath, e);
            throw new IllegalStateException("Could not initialize the database", e);
        }
    }

    /** Returns a new connection with foreign key enforcement enabled. */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void initialize() throws IOException, SQLException {
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.exists(databasePath)) {
            return;
        }
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sqlStatement : splitSchema()) {
                if (!sqlStatement.isBlank()) {
                    statement.execute(sqlStatement);
                }
            }
            log.info("Created database at {}", databasePath);
        }
    }

    private String[] splitSchema() throws IOException {
        String schema;
        try (InputStream input = getClass().getResourceAsStream("/db.sql")) {
            if (input == null) {
                throw new IOException("Schema resource /db.sql not found");
            }
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return schema.split(";");
    }
}