package com.egen.fitogen.database;

import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.repository.EppoZoneRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteEppoZoneRepository
        extends BaseSqliteRepository
        implements EppoZoneRepository {

    @Override
    public List<EppoZone> findAll() {
        List<EppoZone> list = new ArrayList<>();
        String sql = "SELECT * FROM eppo_zones ORDER BY code COLLATE NOCASE";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    @Override
    public EppoZone findById(int id) {
        String sql = "SELECT * FROM eppo_zones WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, id);
            rs = executeQuery(stmt);

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return null;
    }

    @Override
    public EppoZone findByCode(String code) {
        String sql = "SELECT * FROM eppo_zones WHERE lower(code) = lower(?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, code);
            rs = executeQuery(stmt);

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return null;
    }

    @Override
    public void save(EppoZone eppoZone) {
        String sql = """
                INSERT INTO eppo_zones
                (code, name, country_code, status)
                VALUES (?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, eppoZone.getCode());
            stmt.setString(2, eppoZone.getName());
            stmt.setString(3, eppoZone.getCountryCode());
            stmt.setString(4, eppoZone.getStatus());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(EppoZone eppoZone) {
        String sql = """
                UPDATE eppo_zones
                SET code = ?, name = ?, country_code = ?, status = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, eppoZone.getCode());
            stmt.setString(2, eppoZone.getName());
            stmt.setString(3, eppoZone.getCountryCode());
            stmt.setString(4, eppoZone.getStatus());
            stmt.setInt(5, eppoZone.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM eppo_zones WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, id);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    private EppoZone mapRow(ResultSet rs) throws Exception {
        return new EppoZone(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("country_code"),
                rs.getString("status")
        );
    }
}