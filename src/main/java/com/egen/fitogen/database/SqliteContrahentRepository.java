package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteContrahentRepository implements ContrahentRepository {

    @Override
    public List<Contrahent> findAll() {

        List<Contrahent> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM contrahents");
             ResultSet rs = ps.executeQuery()) {

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
        }

        return list;
    }

    @Override
    public void save(Contrahent c) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO contrahents(name,country,country_code,postal_code,city,street,phytosanitary_number) VALUES (?,?,?,?,?,?,?)")) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getCountry());
            ps.setString(3, c.getCountryCode());
            ps.setString(4, c.getPostalCode());
            ps.setString(5, c.getCity());
            ps.setString(6, c.getStreet());
            ps.setString(7, c.getPhytosanitaryNumber());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}