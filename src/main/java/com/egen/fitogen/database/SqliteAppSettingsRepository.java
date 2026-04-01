package com.egen.fitogen.database;

import com.egen.fitogen.repository.AppSettingsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SqliteAppSettingsRepository extends BaseSqliteRepository implements AppSettingsRepository {

    @Override
    public Optional<String> findValueByKey(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read app setting: " + key, e);
        }

        return Optional.empty();
    }

    @Override
    public void upsert(String key, String value) {
        String sql = """
                INSERT INTO app_settings (setting_key, setting_value)
                VALUES (?, ?)
                ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save app setting: " + key, e);
        }
    }

    @Override
    public Map<String, String> findAll() {
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        Map<String, String> result = new HashMap<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                result.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read app settings.", e);
        }

        return result;
    }
}