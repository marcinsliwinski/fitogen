package com.egen.fitogen.database;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.repository.NumberingConfigRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqliteNumberingConfigRepository
        extends BaseSqliteRepository
        implements NumberingConfigRepository {

    @Override
    public NumberingConfig findByType(NumberingType type) {

        String sql = "SELECT * FROM numbering_config WHERE type = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, type.name());

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
    public boolean existsByType(NumberingType type) {

        String sql = "SELECT 1 FROM numbering_config WHERE type = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, type.name());

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
    public void save(NumberingConfig config) {

        String sql = """
                INSERT INTO numbering_config (
                    type,
                    section1_type,
                    section1_static_value,
                    section1_separator,
                    section2_type,
                    section2_static_value,
                    section2_separator,
                    section3_type,
                    section3_static_value,
                    section3_separator,
                    current_counter
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            fillStatement(stmt, config);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                config.setId(rs.getInt(1));
            }
            close(rs);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Nie udało się zapisać konfiguracji numeratora do bazy.", e);
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(NumberingConfig config) {

        String sql = """
                UPDATE numbering_config
                SET
                    type = ?,
                    section1_type = ?,
                    section1_static_value = ?,
                    section1_separator = ?,
                    section2_type = ?,
                    section2_static_value = ?,
                    section2_separator = ?,
                    section3_type = ?,
                    section3_static_value = ?,
                    section3_separator = ?,
                    current_counter = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            fillStatement(stmt, config);
            stmt.setInt(12, config.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Nie udało się zaktualizować konfiguracji numeratora w bazie.", e);
        } finally {
            close(stmt, conn);
        }
    }

    private void fillStatement(PreparedStatement stmt, NumberingConfig config) throws Exception {

        stmt.setString(1, config.getType() != null ? config.getType().name() : null);

        stmt.setString(2, enumName(config.getSection1Type()));
        stmt.setString(3, config.getSection1StaticValue());
        stmt.setString(4, config.getSection1Separator());

        stmt.setString(5, enumName(config.getSection2Type()));
        stmt.setString(6, config.getSection2StaticValue());
        stmt.setString(7, config.getSection2Separator());

        stmt.setString(8, enumName(config.getSection3Type()));
        stmt.setString(9, config.getSection3StaticValue());
        stmt.setString(10, config.getSection3Separator());

        stmt.setInt(11, config.getCurrentCounter());
    }

    private NumberingConfig mapRow(ResultSet rs) throws Exception {

        NumberingConfig config = new NumberingConfig();

        config.setId(rs.getInt("id"));
        config.setType(NumberingType.valueOf(rs.getString("type")));

        config.setSection1Type(toSectionType(rs.getString("section1_type")));
        config.setSection1StaticValue(rs.getString("section1_static_value"));
        config.setSection1Separator(rs.getString("section1_separator"));

        config.setSection2Type(toSectionType(rs.getString("section2_type")));
        config.setSection2StaticValue(rs.getString("section2_static_value"));
        config.setSection2Separator(rs.getString("section2_separator"));

        config.setSection3Type(toSectionType(rs.getString("section3_type")));
        config.setSection3StaticValue(rs.getString("section3_static_value"));
        config.setSection3Separator(rs.getString("section3_separator"));

        config.setCurrentCounter(rs.getInt("current_counter"));

        return config;
    }

    private String enumName(NumberingSectionType value) {
        return value != null ? value.name() : null;
    }

    private NumberingSectionType toSectionType(String value) {
        return value != null && !value.isBlank()
                ? NumberingSectionType.valueOf(value)
                : null;
    }
}
