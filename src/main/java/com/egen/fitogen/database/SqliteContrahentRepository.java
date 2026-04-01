package com.egen.fitogen.database;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteContrahentRepository
        extends BaseSqliteRepository
        implements ContrahentRepository {

    @Override
    public void save(Contrahent contrahent) {
        String sql = """
                INSERT INTO contrahents
                (name, country, country_code, postal_code, city, street, phytosanitary_number, is_supplier, is_client)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, contrahent.getName());
            stmt.setString(2, contrahent.getCountry());
            stmt.setString(3, contrahent.getCountryCode());
            stmt.setString(4, contrahent.getPostalCode());
            stmt.setString(5, contrahent.getCity());
            stmt.setString(6, contrahent.getStreet());
            stmt.setString(7, contrahent.getPhytosanitaryNumber());
            stmt.setBoolean(8, contrahent.isSupplier());
            stmt.setBoolean(9, contrahent.isClient());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(Contrahent contrahent) {
        String sql = """
                UPDATE contrahents
                SET name = ?, country = ?, country_code = ?, postal_code = ?, city = ?, street = ?,
                    phytosanitary_number = ?, is_supplier = ?, is_client = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, contrahent.getName());
            stmt.setString(2, contrahent.getCountry());
            stmt.setString(3, contrahent.getCountryCode());
            stmt.setString(4, contrahent.getPostalCode());
            stmt.setString(5, contrahent.getCity());
            stmt.setString(6, contrahent.getStreet());
            stmt.setString(7, contrahent.getPhytosanitaryNumber());
            stmt.setBoolean(8, contrahent.isSupplier());
            stmt.setBoolean(9, contrahent.isClient());
            stmt.setInt(10, contrahent.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM contrahents WHERE id = ?";

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
    public Contrahent findById(int id) {
        String sql = "SELECT * FROM contrahents WHERE id = ?";

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
    public List<Contrahent> findAll() {
        List<Contrahent> list = new ArrayList<>();
        String sql = "SELECT * FROM contrahents ORDER BY name COLLATE NOCASE";

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
    public List<Contrahent> findAllClients() {
        List<Contrahent> list = new ArrayList<>();
        String sql = """
                SELECT * FROM contrahents
                WHERE COALESCE(is_client, 0) = 1
                ORDER BY name COLLATE NOCASE
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

    private Contrahent mapRow(ResultSet rs) throws Exception {
        Contrahent contrahent = new Contrahent();
        contrahent.setId(rs.getInt("id"));
        contrahent.setName(rs.getString("name"));
        contrahent.setCountry(rs.getString("country"));
        contrahent.setCountryCode(rs.getString("country_code"));
        contrahent.setPostalCode(rs.getString("postal_code"));
        contrahent.setCity(rs.getString("city"));
        contrahent.setStreet(rs.getString("street"));
        contrahent.setPhytosanitaryNumber(rs.getString("phytosanitary_number"));
        contrahent.setSupplier(rs.getBoolean("is_supplier"));
        contrahent.setClient(rs.getBoolean("is_client"));
        return contrahent;
    }
}