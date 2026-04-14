package com.egen.fitogen.database;

import com.egen.fitogen.model.Plant;
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
                        rs.getString("eppo_code"),
                        rs.getInt("passport_required") == 1,
                        rs.getString("visibility_status"),
                        rs.getString("default_document_type")
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
                        rs.getString("eppo_code"),
                        rs.getInt("passport_required") == 1,
                        rs.getString("visibility_status"),
                        rs.getString("default_document_type")
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
                (species, variety, rootstock, latin_species_name, eppo_code, passport_required, visibility_status, default_document_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setString(5, plant.getEppoCode());
            stmt.setInt(6, plant.isPassportRequired() ? 1 : 0);
            stmt.setString(7, plant.getVisibilityStatus());
            stmt.setString(8, plant.getDefaultDocumentType());

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
                SET species=?, variety=?, rootstock=?, latin_species_name=?, eppo_code=?, passport_required=?, visibility_status=?, default_document_type=?
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
            stmt.setString(5, plant.getEppoCode());
            stmt.setInt(6, plant.isPassportRequired() ? 1 : 0);
            stmt.setString(7, plant.getVisibilityStatus());
            stmt.setString(8, plant.getDefaultDocumentType());
            stmt.setInt(9, plant.getId());

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
    @Override
    public boolean existsByIdentityExcludingId(String species, String variety, String rootstock, int excludedPlantId) {

        String sql = """
            SELECT 1
            FROM plants
            WHERE lower(trim(coalesce(species, ''))) = lower(trim(coalesce(?, '')))
              AND lower(trim(coalesce(variety, ''))) = lower(trim(coalesce(?, '')))
              AND lower(trim(coalesce(rootstock, ''))) = lower(trim(coalesce(?, '')))
              AND id <> ?
            LIMIT 1
            """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, species);
            stmt.setString(2, variety);
            stmt.setString(3, rootstock);
            stmt.setInt(4, excludedPlantId);

            rs = executeQuery(stmt);
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return false;
    }
}