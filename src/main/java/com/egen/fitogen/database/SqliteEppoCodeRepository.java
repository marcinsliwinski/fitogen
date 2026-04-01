package com.egen.fitogen.database;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.repository.EppoCodeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteEppoCodeRepository
        extends BaseSqliteRepository
        implements EppoCodeRepository {

    @Override
    public List<EppoCode> findAll() {
        List<EppoCode> list = new ArrayList<>();
        String sql = "SELECT * FROM eppo_codes ORDER BY code COLLATE NOCASE";

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
    public EppoCode findById(int id) {
        String sql = "SELECT * FROM eppo_codes WHERE id = ?";

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
    public EppoCode findByCode(String code) {
        String sql = "SELECT * FROM eppo_codes WHERE lower(code) = lower(?)";

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
    public void save(EppoCode eppoCode) {
        String sql = """
                INSERT INTO eppo_codes
                (code, species_name, latin_species_name, scientific_name, common_name, passport_required, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, eppoCode.getCode());
            stmt.setString(2, eppoCode.getSpeciesName());
            stmt.setString(3, eppoCode.getLatinSpeciesName());
            stmt.setString(4, eppoCode.getScientificName());
            stmt.setString(5, eppoCode.getCommonName());
            stmt.setInt(6, eppoCode.isPassportRequired() ? 1 : 0);
            stmt.setString(7, eppoCode.getStatus());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(EppoCode eppoCode) {
        String sql = """
                UPDATE eppo_codes
                SET code = ?, species_name = ?, latin_species_name = ?, scientific_name = ?, common_name = ?, passport_required = ?, status = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, eppoCode.getCode());
            stmt.setString(2, eppoCode.getSpeciesName());
            stmt.setString(3, eppoCode.getLatinSpeciesName());
            stmt.setString(4, eppoCode.getScientificName());
            stmt.setString(5, eppoCode.getCommonName());
            stmt.setInt(6, eppoCode.isPassportRequired() ? 1 : 0);
            stmt.setString(7, eppoCode.getStatus());
            stmt.setInt(8, eppoCode.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM eppo_codes WHERE id = ?";

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

    private EppoCode mapRow(ResultSet rs) throws Exception {
        return new EppoCode(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("species_name"),
                rs.getString("latin_species_name"),
                rs.getString("scientific_name"),
                rs.getString("common_name"),
                rs.getInt("passport_required") == 1,
                rs.getString("status")
        );
    }
}