package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.repository.PlantBatchRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantBatchRepository implements PlantBatchRepository {

    @Override
    public List<PlantBatch> findAll() {

        List<PlantBatch> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM plant_batches")) {

            while (rs.next()) {

                PlantBatch batch = new PlantBatch();

                batch.setId(rs.getInt("id"));
                batch.setPlantId(rs.getInt("plant_id"));
                batch.setInteriorBatchNo(rs.getString("interior_batch_no"));
                batch.setExteriorBatchNo(rs.getString("exterior_batch_no"));
                batch.setQty(rs.getInt("qty"));
                batch.setCreationDate(Date.valueOf(rs.getString("creation_date")).toLocalDate());
                batch.setManufacturerCountryCode(rs.getString("manufacturer_country_code"));
                batch.setFitoQualificationCategory(rs.getString("fito_qualification_category"));
                batch.setEppoCode(rs.getString("eppo_code"));
                batch.setZpZone(rs.getString("zp_zone"));
                batch.setContrahentId(rs.getInt("contrahent_id"));
                batch.setComments(rs.getString("comments"));

                list.add(batch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public PlantBatch findById(int id) {
        return null;
    }

    @Override
    public void save(PlantBatch batch) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO plant_batches(plant_id,interior_batch_no,qty) VALUES(?,?,?)")) {

            ps.setInt(1, batch.getPlantId());
            ps.setString(2, batch.getInteriorBatchNo());
            ps.setInt(3, batch.getQty());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(PlantBatch batch) {}

    @Override
    public void delete(int id) {}
}