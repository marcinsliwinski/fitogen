package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                c.setCountryCode(rs.getString("country_code"));
                c.setNip(rs.getString("nip"));
                list.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Contrahent findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM contrahents WHERE id = ?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Contrahent c = new Contrahent();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCountryCode(rs.getString("country_code"));
                c.setNip(rs.getString("nip"));
                return c;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Contrahent> findByName(String name) {
        List<Contrahent> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM contrahents WHERE name LIKE ?")) {

            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Contrahent c = new Contrahent();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCountryCode(rs.getString("country_code"));
                c.setNip(rs.getString("nip"));
                list.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void save(Contrahent contrahent) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO contrahents(name, country_code, nip) VALUES(?,?,?)")) {

            ps.setString(1, contrahent.getName());
            ps.setString(2, contrahent.getCountryCode());
            ps.setString(3, contrahent.getNip());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Contrahent contrahent) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contrahents SET name=?, country_code=?, nip=? WHERE id=?")) {

            ps.setString(1, contrahent.getName());
            ps.setString(2, contrahent.getCountryCode());
            ps.setString(3, contrahent.getNip());
            ps.setInt(4, contrahent.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM contrahents WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}