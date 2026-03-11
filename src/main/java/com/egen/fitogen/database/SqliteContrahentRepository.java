package com.egen.fitogen.database;

import com.egen.fitogen.domain.Contrahent;
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
    public void save(Contrahent c) {

        String sql = """
                INSERT INTO contrahents
                (name,country,country_code,postal_code,city,street,phytosanitary_number)
                VALUES(?,?,?,?,?,?,?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, c.getName());
            stmt.setString(2, c.getCountry());
            stmt.setString(3, c.getCountryCode());
            stmt.setString(4, c.getPostalCode());
            stmt.setString(5, c.getCity());
            stmt.setString(6, c.getStreet());
            stmt.setString(7, c.getPhytosanitaryNumber());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public List<Contrahent> findAll() {

        List<Contrahent> list = new ArrayList<>();

        String sql = "SELECT * FROM contrahents";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {

                Contrahent c = new Contrahent();

                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCountry(rs.getString("country"));
                c.setCountryCode(rs.getString("country_code"));
                c.setPostalCode(rs.getString("postal_code"));
                c.setCity(rs.getString("city"));
                c.setStreet(rs.getString("street"));
                c.setPhytosanitaryNumber(rs.getString("phytosanitary_number"));

                list.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }
}