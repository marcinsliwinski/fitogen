package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseSqliteRepository {

    protected Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    protected PreparedStatement prepare(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    protected ResultSet executeQuery(PreparedStatement stmt) throws SQLException {
        return stmt.executeQuery();
    }

    protected int executeUpdate(PreparedStatement stmt) throws SQLException {
        return stmt.executeUpdate();
    }

    protected void close(AutoCloseable... resources) {

        for (AutoCloseable r : resources) {

            if (r != null) {
                try {
                    r.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}