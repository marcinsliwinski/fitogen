package com.egen.fitogen.database;

import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.repository.PlantRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantRepository
        extends BaseSqliteRepository
        implements PlantRepository {

    @Override
    public List<Plant> findAll() {

        List<Plant> plants = new ArrayList<>();

        String sql = "SELECT * FROM plants";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {

                Plant plant = new Plant(
                        rs.getInt("id"),
                        rs.getString("species"),
                        rs.getString("variety"),
                        rs.getString("rootstock"),
                        rs.getString("latin_species_name"),
                        rs.getString("visibility_status")
                );

                plants.add(plant);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return plants;
    }

    @Override
    public Plant findById(int id) {

        String sql = "SELECT * FROM plants WHERE id=?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setInt(1, id);

            rs = executeQuery(stmt);

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

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return null;
    }

    @Override
    public void save(Plant plant) {

        String sql = """
                INSERT INTO plants
                (species, variety, rootstock, latin_species_name, visibility_status)
                VALUES (?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, plant.getSpecies());
            stmt.setString(2, plant.getVariety());
            stmt.setString(3, plant.getRootstock());
            stmt.setString(4, plant.getLatinSpeciesName());
            stmt.setString(5, plant.getVisibilityStatus());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(Plant plant) {

        String sql = """
                UPDATE plants
                SET species=?, variety=?, rootstock=?, latin_species_name=?, visibility_status=?
                WHERE id=?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, plant.getSpecies());
            stmt.setString(2, plant.getVariety());
            stmt.setString(3, plant.getRootstock());
            stmt.setString(4, plant.getLatinSpeciesName());
            stmt.setString(5, plant.getVisibilityStatus());
            stmt.setInt(6, plant.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void delete(int id) {

        String sql = "DELETE FROM plants WHERE id=?";

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
}