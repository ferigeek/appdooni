package com.github.ferigeek.appdooni.repository;

import com.github.ferigeek.appdooni.model.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistence for {@link OperatingSystem} entities. */
public final class OperatingSystemRepository {

    private static final Logger log = LoggerFactory.getLogger(OperatingSystemRepository.class);
    private final DatabaseManager databaseManager;

    public OperatingSystemRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<OperatingSystem> findAll() {
        String sql = "SELECT id, name FROM operating_systems ORDER BY name";
        List<OperatingSystem> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
        } catch (SQLException e) {
            log.error("Failed to list operating systems", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    public Optional<OperatingSystem> findById(int id) {
        String sql = "SELECT id, name FROM operating_systems WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find operating system {}", id, e);
            throw new IllegalStateException(e);
        }
        return Optional.empty();
    }

    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM operating_systems WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check operating system name {}", name, e);
            throw new IllegalStateException(e);
        }
    }

    public Optional<OperatingSystem> findByName(String name) {
        String sql = "SELECT id, name FROM operating_systems WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find operating system by name {}", name, e);
            throw new IllegalStateException(e);
        }
        return Optional.empty();
    }

    /** Inserts the operating system and returns it with its generated id. */
    public OperatingSystem insert(OperatingSystem operatingSystem) {
        String sql = "INSERT INTO operating_systems (name) VALUES (?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, operatingSystem.getName());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    operatingSystem.setId(keys.getInt(1));
                }
            }
            log.info("Added operating system '{}'", operatingSystem.getName());
            return operatingSystem;
        } catch (SQLException e) {
            log.error("Failed to insert operating system '{}'", operatingSystem.getName(), e);
            throw new IllegalStateException(e);
        }
    }

    public void update(OperatingSystem operatingSystem) {
        String sql = "UPDATE operating_systems SET name = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operatingSystem.getName());
            statement.setInt(2, operatingSystem.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update operating system {}", operatingSystem.getId(), e);
            throw new IllegalStateException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM operating_systems WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete operating system {}", id, e);
            throw new IllegalStateException(e);
        }
    }

    private OperatingSystem map(ResultSet resultSet) throws SQLException {
        return new OperatingSystem(resultSet.getInt("id"), resultSet.getString("name"));
    }
}