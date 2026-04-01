package com.egen.fitogen.database;

import com.egen.fitogen.model.EppoCodePlantLink;
import com.egen.fitogen.repository.EppoCodePlantLinkRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteEppoCodePlantLinkRepository
        extends BaseSqliteRepository
        implements EppoCodePlantLinkRepository {

    @Override
    public List<EppoCodePlantLink> findAll() {
        List<EppoCodePlantLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, plant_id
                FROM eppo_code_plant_links
                ORDER BY eppo_code_id, plant_id
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
    public List<EppoCodePlantLink> findByEppoCodeId(int eppoCodeId) {
        List<EppoCodePlantLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, plant_id
                FROM eppo_code_plant_links
                WHERE eppo_code_id = ?
                ORDER BY plant_id
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
    public List<EppoCodePlantLink> findByPlantId(int plantId) {
        List<EppoCodePlantLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, plant_id
                FROM eppo_code_plant_links
                WHERE plant_id = ?
                ORDER BY eppo_code_id
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, plantId);
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
    public boolean exists(int eppoCodeId, int plantId) {
        String sql = """
                SELECT 1
                FROM eppo_code_plant_links
                WHERE eppo_code_id = ?
                  AND plant_id = ?
                LIMIT 1
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            stmt.setInt(2, plantId);
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
    public void save(EppoCodePlantLink link) {
        String sql = """
                INSERT INTO eppo_code_plant_links (eppo_code_id, plant_id)
                VALUES (?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, link.getEppoCodeId());
            stmt.setInt(2, link.getPlantId());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM eppo_code_plant_links WHERE id = ?";

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
    public void deleteByPair(int eppoCodeId, int plantId) {
        String sql = """
                DELETE FROM eppo_code_plant_links
                WHERE eppo_code_id = ?
                  AND plant_id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            stmt.setInt(2, plantId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteByEppoCodeId(int eppoCodeId) {
        String sql = "DELETE FROM eppo_code_plant_links WHERE eppo_code_id = ?";

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
    public void deleteByPlantId(int plantId) {
        String sql = "DELETE FROM eppo_code_plant_links WHERE plant_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, plantId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    private EppoCodePlantLink mapRow(ResultSet rs) throws Exception {
        return new EppoCodePlantLink(
                rs.getInt("id"),
                rs.getInt("eppo_code_id"),
                rs.getInt("plant_id")
        );
    }
}