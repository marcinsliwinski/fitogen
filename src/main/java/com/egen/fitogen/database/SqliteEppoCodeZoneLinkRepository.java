package com.egen.fitogen.database;

import com.egen.fitogen.model.EppoCodeZoneLink;
import com.egen.fitogen.repository.EppoCodeZoneLinkRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteEppoCodeZoneLinkRepository
        extends BaseSqliteRepository
        implements EppoCodeZoneLinkRepository {

    @Override
    public List<EppoCodeZoneLink> findAll() {
        List<EppoCodeZoneLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, eppo_zone_id
                FROM eppo_code_zone_links
                ORDER BY eppo_code_id, eppo_zone_id
                """;

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
    public List<EppoCodeZoneLink> findByEppoCodeId(int eppoCodeId) {
        List<EppoCodeZoneLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, eppo_zone_id
                FROM eppo_code_zone_links
                WHERE eppo_code_id = ?
                ORDER BY eppo_zone_id
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
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
    public List<EppoCodeZoneLink> findByEppoZoneId(int eppoZoneId) {
        List<EppoCodeZoneLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, eppo_zone_id
                FROM eppo_code_zone_links
                WHERE eppo_zone_id = ?
                ORDER BY eppo_code_id
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoZoneId);
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
    public boolean exists(int eppoCodeId, int eppoZoneId) {
        String sql = """
                SELECT 1
                FROM eppo_code_zone_links
                WHERE eppo_code_id = ?
                  AND eppo_zone_id = ?
                LIMIT 1
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            stmt.setInt(2, eppoZoneId);
            rs = executeQuery(stmt);

            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return false;
    }

    @Override
    public void save(EppoCodeZoneLink link) {
        String sql = """
                INSERT INTO eppo_code_zone_links (eppo_code_id, eppo_zone_id)
                VALUES (?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, link.getEppoCodeId());
            stmt.setInt(2, link.getEppoZoneId());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM eppo_code_zone_links WHERE id = ?";

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

    @Override
    public void deleteByPair(int eppoCodeId, int eppoZoneId) {
        String sql = """
                DELETE FROM eppo_code_zone_links
                WHERE eppo_code_id = ?
                  AND eppo_zone_id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            stmt.setInt(2, eppoZoneId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteByEppoCodeId(int eppoCodeId) {
        String sql = "DELETE FROM eppo_code_zone_links WHERE eppo_code_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteByEppoZoneId(int eppoZoneId) {
        String sql = "DELETE FROM eppo_code_zone_links WHERE eppo_zone_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoZoneId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    private EppoCodeZoneLink mapRow(ResultSet rs) throws Exception {
        return new EppoCodeZoneLink(
                rs.getInt("id"),
                rs.getInt("eppo_code_id"),
                rs.getInt("eppo_zone_id")
        );
    }
}