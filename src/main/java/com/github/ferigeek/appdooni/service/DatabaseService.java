package com.github.ferigeek.appdooni.service;

import com.github.ferigeek.appdooni.repository.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Export and import of the application database. Export copies the database
 * file; import merges the contents of another database into the current one.
 * Conflicting entries are resolved through an {@link ImportConflictResolver}.
 */
public final class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DatabaseManager databaseManager;

    public DatabaseService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /** Copies the current database to {@code targetDirectory} as {@code appdooni-<timestamp>.db}. */
    public Path export(Path targetDirectory) throws IOException {
        Path source = databaseManager.getDatabasePath();
        Path destination = targetDirectory.resolve("appdooni-" + LocalDateTime.now().format(TIMESTAMP) + ".db");
        Files.createDirectories(targetDirectory);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("Exported database to {}", destination);
        return destination;
    }

    /**
     * Merges the contents of {@code source} into the current database. Operating
     * systems and tags are matched by their unique name; an existing entry is
     * reused, otherwise a new one is inserted. Applications are matched by name
     * and resolved through the given resolver.
     */
    public void importDatabase(Path source, ImportConflictResolver resolver) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Not a valid database file: " + source);
        }
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement attach = connection.prepareStatement("ATTACH DATABASE ? AS imported")) {
                attach.setString(1, source.toString());
                attach.execute();
            }
            try {
                Map<Integer, Integer> osMapping = importLookupTable(connection, resolver, "OS", "operating_systems");
                Map<Integer, Integer> tagMapping = importLookupTable(connection, resolver, "Tag", "tags");
                importApplications(connection, resolver, osMapping, tagMapping);
                connection.commit();
                log.info("Imported database from {}", source);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                try (PreparedStatement detach = connection.prepareStatement("DETACH DATABASE imported")) {
                    detach.execute();
                } catch (SQLException ignored) {
                    // already detached on failure
                }
            }
        } catch (SQLException e) {
            throw new IOException("Failed to import database " + source, e);
        }
    }

    /**
     * Imports a single lookup table (operating systems or tags), mapping each
     * source id to the id that will be used in the current database.
     */
    private Map<Integer, Integer> importLookupTable(Connection connection, ImportConflictResolver resolver,
                                                    String type, String table) throws SQLException {
        Map<Integer, Integer> mapping = new HashMap<>();
        try (ResultSet rows = connection.createStatement().executeQuery(
                "SELECT id, name FROM imported." + table + " ORDER BY name")) {
            while (rows.next()) {
                int sourceId = rows.getInt("id");
                String name = rows.getString("name");
                Integer existing = findIdByName(connection, table, name);
                if (existing != null) {
                    mapping.put(sourceId, existing);
                    continue;
                }
                int newId = insertName(connection, table, name);
                mapping.put(sourceId, newId);
            }
        }
        return mapping;
    }

    private void importApplications(Connection connection, ImportConflictResolver resolver,
                                    Map<Integer, Integer> osMapping, Map<Integer, Integer> tagMapping)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id, name, description, installation_source, website_url FROM imported.applications");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                int sourceId = rows.getInt("id");
                String name = rows.getString("name");
                Integer existing = findIdByName(connection, "applications", name);
                int targetAppId;
                if (existing != null) {
                    ImportAction action = resolver.resolve("App", name);
                    switch (action) {
                        case SKIP -> { continue; }
                        case OVERWRITE -> {
                            updateApplication(connection, existing, rows);
                            clearAssociations(connection, existing);
                            targetAppId = existing;
                        }
                        default -> targetAppId = insertApplication(connection, rows);
                    }
                } else {
                    targetAppId = insertApplication(connection, rows);
                }
                copyAssociations(connection, sourceId, targetAppId, osMapping, tagMapping);
            }
        }
    }

    private void clearAssociations(Connection connection, int applicationId) throws SQLException {
        try (PreparedStatement clearOs = connection.prepareStatement(
                "DELETE FROM application_operating_systems WHERE application_id = ?");
             PreparedStatement clearTags = connection.prepareStatement(
                "DELETE FROM application_tags WHERE application_id = ?")) {
            clearOs.setInt(1, applicationId);
            clearOs.executeUpdate();
            clearTags.setInt(1, applicationId);
            clearTags.executeUpdate();
        }
    }

    private void copyAssociations(Connection connection, int sourceAppId, int targetAppId,
                                  Map<Integer, Integer> osMapping, Map<Integer, Integer> tagMapping)
            throws SQLException {
        try (PreparedStatement selectOs = connection.prepareStatement(
                "SELECT operating_system_id FROM imported.application_operating_systems WHERE application_id = ?");
             PreparedStatement insertOs = connection.prepareStatement(
                "INSERT OR IGNORE INTO application_operating_systems (application_id, operating_system_id) VALUES (?, ?)")) {
            selectOs.setInt(1, sourceAppId);
            try (ResultSet osRows = selectOs.executeQuery()) {
                while (osRows.next()) {
                    Integer targetOs = osMapping.get(osRows.getInt("operating_system_id"));
                    if (targetOs != null) {
                        insertOs.setInt(1, targetAppId);
                        insertOs.setInt(2, targetOs);
                        insertOs.executeUpdate();
                    }
                }
            }
        }
        try (PreparedStatement selectTag = connection.prepareStatement(
                "SELECT tag_id FROM imported.application_tags WHERE application_id = ?");
             PreparedStatement insertTag = connection.prepareStatement(
                "INSERT OR IGNORE INTO application_tags (application_id, tag_id) VALUES (?, ?)")) {
            selectTag.setInt(1, sourceAppId);
            try (ResultSet tagRows = selectTag.executeQuery()) {
                while (tagRows.next()) {
                    Integer targetTag = tagMapping.get(tagRows.getInt("tag_id"));
                    if (targetTag != null) {
                        insertTag.setInt(1, targetAppId);
                        insertTag.setInt(2, targetTag);
                        insertTag.executeUpdate();
                    }
                }
            }
        }
    }

    private Integer findIdByName(Connection connection, String table, String name) throws SQLException {
        String sql = "SELECT id FROM " + table + " WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        return null;
    }

    private int insertName(Connection connection, String table, String name) throws SQLException {
        String sql = "INSERT INTO " + table + " (name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
        return findIdByName(connection, table, name);
    }

    private int insertApplication(Connection connection, ResultSet source) throws SQLException {
        String sql = "INSERT INTO applications (name, description, installation_source, website_url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.getString("name"));
            statement.setString(2, source.getString("description"));
            statement.setString(3, source.getString("installation_source"));
            statement.setString(4, source.getString("website_url"));
            statement.executeUpdate();
        }
        return findIdByName(connection, "applications", source.getString("name"));
    }

    private void updateApplication(Connection connection, int id, ResultSet source) throws SQLException {
        String sql = "UPDATE applications SET name = ?, description = ?, installation_source = ?, website_url = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.getString("name"));
            statement.setString(2, source.getString("description"));
            statement.setString(3, source.getString("installation_source"));
            statement.setString(4, source.getString("website_url"));
            statement.setInt(5, id);
            statement.executeUpdate();
        }
    }
}