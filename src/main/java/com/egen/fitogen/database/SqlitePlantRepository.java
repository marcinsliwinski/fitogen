package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.repository.PlantRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantRepository implements PlantRepository {

    @Override
    public List<Plant> findAll() {
        List<Plant> plants = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM plants")) {

            while (rs.next()) {
                plants.add(new Plant(
                        rs.getInt("id"),
                        rs.getString("species"),
                        rs.getString("variety"),
                        rs.getString("rootstock"),
                        rs.getString("latin_species_name"),
                        rs.getString("visibility_status")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return plants;
    }

    @Override
    public Plant findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM plants WHERE id=?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Plant(
                        rs.getInt("id"),
                        rs.getString("species"),
                        rs.getString("variety"),
                        rs.getString("rootstock"),
                        rs.getString("latin_species_name"),
                        rs.getString("visibility_status")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void save(Plant plant) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO plants(species,variety,rootstock,latin_species_name,visibility_status) VALUES(?,?,?,?,?)")) {

            ps.setString(1, plant.getSpecies());
            ps.setString(2, plant.getVariety());
            ps.setString(3, plant.getRootstock());
            ps.setString(4, plant.getLatinSpeciesName());
            ps.setString(5, plant.getVisibilityStatus());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Plant plant) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE plants SET species=?, variety=?, rootstock=?, latin_species_name=?, visibility_status=? WHERE id=?")) {

            ps.setString(1, plant.getSpecies());
            ps.setString(2, plant.getVariety());
            ps.setString(3, plant.getRootstock());
            ps.setString(4, plant.getLatinSpeciesName());
            ps.setString(5, plant.getVisibilityStatus());
            ps.setInt(6, plant.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM plants WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}