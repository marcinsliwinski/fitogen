package com.egen.fitogen.database;

import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.repository.AppUserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteAppUserRepository
        extends BaseSqliteRepository
        implements AppUserRepository {

    private static final String DEFAULT_USER_KEY = "default_user_id";

    @Override
    public List<AppUser> findAll() {
        List<AppUser> list = new ArrayList<>();
        String sql = "SELECT * FROM app_users ORDER BY last_name COLLATE NOCASE, first_name COLLATE NOCASE";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {
                AppUser user = new AppUser();
                user.setId(rs.getInt("id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                list.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    @Override
    public void save(AppUser user) {
        String sql = "INSERT INTO app_users (first_name, last_name) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(AppUser user) {
        String sql = "UPDATE app_users SET first_name = ?, last_name = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setInt(3, user.getId());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        Connection conn = null;
        PreparedStatement deleteUserStmt = null;
        PreparedStatement clearDefaultStmt = null;

        try {
            conn = getConnection();

            clearDefaultStmt = prepare(conn,
                    "DELETE FROM app_settings WHERE setting_key = ? AND setting_value = ?");
            clearDefaultStmt.setString(1, DEFAULT_USER_KEY);
            clearDefaultStmt.setString(2, String.valueOf(id));
            executeUpdate(clearDefaultStmt);

            deleteUserStmt = prepare(conn, "DELETE FROM app_users WHERE id = ?");
            deleteUserStmt.setInt(1, id);
            executeUpdate(deleteUserStmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(clearDefaultStmt);
            close(deleteUserStmt, conn);
        }
    }

    @Override
    public Optional<Integer> findDefaultUserId() {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, DEFAULT_USER_KEY);
            rs = executeQuery(stmt);

            if (rs.next()) {
                String value = rs.getString("setting_value");
                if (value != null && !value.isBlank()) {
                    return Optional.of(Integer.parseInt(value.trim()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return Optional.empty();
    }

    @Override
    public void saveDefaultUserId(Integer userId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            if (userId == null || userId <= 0) {
                stmt = prepare(conn, "DELETE FROM app_settings WHERE setting_key = ?");
                stmt.setString(1, DEFAULT_USER_KEY);
            } else {
                stmt = prepare(conn,
                        "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) "
                                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value");
                stmt.setString(1, DEFAULT_USER_KEY);
                stmt.setString(2, String.valueOf(userId));
            }

            executeUpdate(stmt);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }
}
