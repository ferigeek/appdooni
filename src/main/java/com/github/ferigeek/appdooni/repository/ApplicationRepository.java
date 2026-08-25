package com.github.ferigeek.appdooni.repository;

import com.github.ferigeek.appdooni.model.Application;
import com.github.ferigeek.appdooni.model.OperatingSystem;
import com.github.ferigeek.appdooni.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence for {@link Application} entities including their OS and tag associations. */
public final class ApplicationRepository {

    private static final Logger log = LoggerFactory.getLogger(ApplicationRepository.class);
    private final DatabaseManager databaseManager;

    public ApplicationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Application> findAll() {
        return find("SELECT id, name, description, installation_source, website_url, created_at, updated_at FROM applications");
    }

    public Optional<Application> findById(int id) {
        String sql = "SELECT id, name, description, installation_source, website_url, created_at, updated_at FROM applications WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Application application = map(resultSet);
                    attachAssociations(connection, List.of(application));
                    return Optional.of(application);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find application {}", id, e);
            throw new IllegalStateException(e);
        }
        return Optional.empty();
    }

    /** Inserts the application with its OS/tag associations and returns it with its id. */
    public Application insert(Application application) {
        String sql = "INSERT INTO applications (name, description, installation_source, website_url) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, application.getName());
                statement.setString(2, application.getDescription());
                statement.setString(3, application.getInstallationSource());
                statement.setString(4, application.getWebsiteUrl());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        application.setId(keys.getInt(1));
                    }
                }
            }
            insertAssociations(connection, application);
            connection.commit();
            log.info("Added application '{}'", application.getName());
            return application;
        } catch (SQLException e) {
            log.error("Failed to insert application '{}'", application.getName(), e);
            throw new IllegalStateException(e);
        }
    }

    /** Updates the basic fields and replaces the OS/tag associations. */
    public void update(Application application) {
        String sql = "UPDATE applications SET name = ?, description = ?, installation_source = ?, website_url = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, application.getName());
                statement.setString(2, application.getDescription());
                statement.setString(3, application.getInstallationSource());
                statement.setString(4, application.getWebsiteUrl());
                statement.setInt(5, application.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement clearOs = connection.prepareStatement(
                    "DELETE FROM application_operating_systems WHERE application_id = ?");
                 PreparedStatement clearTags = connection.prepareStatement(
                    "DELETE FROM application_tags WHERE application_id = ?")) {
                clearOs.setInt(1, application.getId());
                clearOs.executeUpdate();
                clearTags.setInt(1, application.getId());
                clearTags.executeUpdate();
            }
            insertAssociations(connection, application);
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to update application {}", application.getId(), e);
            throw new IllegalStateException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete application {}", id, e);
            throw new IllegalStateException(e);
        }
    }

    private List<Application> find(String baseSql, Object... params) {
        Map<Integer, Application> applications = new LinkedHashMap<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(baseSql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    applications.put(resultSet.getInt("id"), map(resultSet));
                }
            }
            attachAssociations(connection, applications.values());
        } catch (SQLException e) {
            log.error("Failed to list applications", e);
            throw new IllegalStateException(e);
        }
        return new ArrayList<>(applications.values());
    }

    private void attachAssociations(Connection connection, Collection<Application> applications) throws SQLException {
        if (applications.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(applications.size(), "?"));
        try (PreparedStatement osStatement = connection.prepareStatement(
                "SELECT aos.application_id, os.id, os.name FROM application_operating_systems aos " +
                "JOIN operating_systems os ON os.id = aos.operating_system_id " +
                "WHERE aos.application_id IN (" + placeholders + ")");
             PreparedStatement tagStatement = connection.prepareStatement(
                "SELECT at.application_id, t.id, t.name FROM application_tags at " +
                "JOIN tags t ON t.id = at.tag_id " +
                "WHERE at.application_id IN (" + placeholders + ")")) {
            Map<Integer, Application> byId = new LinkedHashMap<>();
            int i = 1;
            for (Application application : applications) {
                osStatement.setInt(i, application.getId());
                tagStatement.setInt(i, application.getId());
                byId.put(application.getId(), application);
                i++;
            }
            try (ResultSet osResult = osStatement.executeQuery()) {
                while (osResult.next()) {
                    byId.get(osResult.getInt("application_id"))
                            .addOperatingSystem(new OperatingSystem(osResult.getInt("id"), osResult.getString("name")));
                }
            }
            try (ResultSet tagResult = tagStatement.executeQuery()) {
                while (tagResult.next()) {
                    byId.get(tagResult.getInt("application_id"))
                            .addTag(new Tag(tagResult.getInt("id"), tagResult.getString("name")));
                }
            }
        }
    }

    private void insertAssociations(Connection connection, Application application) throws SQLException {
        try (PreparedStatement osStatement = connection.prepareStatement(
                "INSERT INTO application_operating_systems (application_id, operating_system_id) VALUES (?, ?)");
             PreparedStatement tagStatement = connection.prepareStatement(
                "INSERT INTO application_tags (application_id, tag_id) VALUES (?, ?)")) {
            for (OperatingSystem os : application.getOperatingSystems()) {
                osStatement.setInt(1, application.getId());
                osStatement.setInt(2, os.getId());
                osStatement.addBatch();
            }
            for (Tag tag : application.getTags()) {
                tagStatement.setInt(1, application.getId());
                tagStatement.setInt(2, tag.getId());
                tagStatement.addBatch();
            }
            osStatement.executeBatch();
            tagStatement.executeBatch();
        }
    }

    private Application map(ResultSet resultSet) throws SQLException {
        Application application = new Application();
        application.setId(resultSet.getInt("id"));
        application.setName(resultSet.getString("name"));
        application.setDescription(resultSet.getString("description"));
        application.setInstallationSource(resultSet.getString("installation_source"));
        application.setWebsiteUrl(resultSet.getString("website_url"));
        application.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
        application.setUpdatedAt(resultSet.getObject("updated_at", LocalDateTime.class));
        return application;
    }
}