package com.egen.fitogen.database;

import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.repository.EppoCodeSpeciesLinkRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteEppoCodeSpeciesLinkRepository
        extends BaseSqliteRepository
        implements EppoCodeSpeciesLinkRepository {

    @Override
    public List<EppoCodeSpeciesLink> findAll() {
        List<EppoCodeSpeciesLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, species_name, latin_species_name
                FROM eppo_code_species_links
                ORDER BY eppo_code_id, species_name, latin_species_name
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
    public List<EppoCodeSpeciesLink> findByEppoCodeId(int eppoCodeId) {
        List<EppoCodeSpeciesLink> list = new ArrayList<>();
        String sql = """
                SELECT id, eppo_code_id, species_name, latin_species_name
                FROM eppo_code_species_links
                WHERE eppo_code_id = ?
                ORDER BY species_name, latin_species_name
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
    public boolean exists(int eppoCodeId, String speciesName, String latinSpeciesName) {
        String sql = """
                SELECT 1
                FROM eppo_code_species_links
                WHERE eppo_code_id = ?
                  AND COALESCE(species_name, '') = COALESCE(?, '')
                  AND COALESCE(latin_species_name, '') = COALESCE(?, '')
                LIMIT 1
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, eppoCodeId);
            stmt.setString(2, speciesName);
            stmt.setString(3, latinSpeciesName);
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
    public void save(EppoCodeSpeciesLink link) {
        String sql = """
                INSERT INTO eppo_code_species_links (eppo_code_id, species_name, latin_species_name)
                VALUES (?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, link.getEppoCodeId());
            stmt.setString(2, link.getSpeciesName());
            stmt.setString(3, link.getLatinSpeciesName());
            executeUpdate(stmt);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM eppo_code_species_links WHERE id = ?";

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
    public void deleteByEppoCodeId(int eppoCodeId) {
        String sql = "DELETE FROM eppo_code_species_links WHERE eppo_code_id = ?";

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

    private EppoCodeSpeciesLink mapRow(ResultSet rs) throws Exception {
        return new EppoCodeSpeciesLink(
                rs.getInt("id"),
                rs.getInt("eppo_code_id"),
                rs.getString("species_name"),
                rs.getString("latin_species_name")
        );
    }
}
