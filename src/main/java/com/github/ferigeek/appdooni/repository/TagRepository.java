package com.github.ferigeek.appdooni.repository;

import com.github.ferigeek.appdooni.model.Tag;
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

/** Persistence for {@link Tag} entities. */
public final class TagRepository {

    private static final Logger log = LoggerFactory.getLogger(TagRepository.class);
    private final DatabaseManager databaseManager;

    public TagRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Tag> findAll() {
        String sql = "SELECT id, name FROM tags ORDER BY name";
        List<Tag> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
        } catch (SQLException e) {
            log.error("Failed to list tags", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    public Optional<Tag> findById(int id) {
        String sql = "SELECT id, name FROM tags WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find tag {}", id, e);
            throw new IllegalStateException(e);
        }
        return Optional.empty();
    }

    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM tags WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check tag name {}", name, e);
            throw new IllegalStateException(e);
        }
    }

    public Optional<Tag> findByName(String name) {
        String sql = "SELECT id, name FROM tags WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find tag by name {}", name, e);
            throw new IllegalStateException(e);
        }
        return Optional.empty();
    }

    /** Inserts the tag and returns it with its generated id. */
    public Tag insert(Tag tag) {
        String sql = "INSERT INTO tags (name) VALUES (?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tag.getName());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    tag.setId(keys.getInt(1));
                }
            }
            log.info("Added tag '{}'", tag.getName());
            return tag;
        } catch (SQLException e) {
            log.error("Failed to insert tag '{}'", tag.getName(), e);
            throw new IllegalStateException(e);
        }
    }

    public void update(Tag tag) {
        String sql = "UPDATE tags SET name = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tag.getName());
            statement.setInt(2, tag.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update tag {}", tag.getId(), e);
            throw new IllegalStateException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete tag {}", id, e);
            throw new IllegalStateException(e);
        }
    }

    private Tag map(ResultSet resultSet) throws SQLException {
        return new Tag(resultSet.getInt("id"), resultSet.getString("name"));
    }
}