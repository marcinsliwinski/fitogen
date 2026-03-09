package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteContrahentRepository implements ContrahentRepository {

    @Override
    public List<Contrahent> findAll() {

        List<Contrahent> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM contrahents")) {

            while (rs.next()) {

                Contrahent c = new Contrahent();

                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCountry(rs.getString("country"));
                c.setCity(rs.getString("city"));
                c.setStreet(rs.getString("street"));
                c.setPostalCode(rs.getString("postal_code"));
                c.setFitosanitaryNumber(rs.getString("fitosanitary_number"));

                list.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public Contrahent findById(int id) { return null; }

    @Override
    public void save(Contrahent c) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO contrahents(name,country,city,street,postal_code,fitosanitary_number) VALUES(?,?,?,?,?,?)")) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getCountry());
            ps.setString(3, c.getCity());
            ps.setString(4, c.getStreet());
            ps.setString(5, c.getPostalCode());
            ps.setString(6, c.getFitosanitaryNumber());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Contrahent c) {}

    @Override
    public void delete(int id) {}
}