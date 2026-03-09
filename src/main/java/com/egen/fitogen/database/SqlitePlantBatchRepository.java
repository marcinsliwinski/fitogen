package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.repository.PlantBatchRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantBatchRepository implements PlantBatchRepository {

    @Override
    public List<PlantBatch> findAll() {

        List<PlantBatch> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM plant_batches");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                PlantBatch batch = new PlantBatch();

                batch.setId(rs.getInt("id"));
                batch.setPlantId(rs.getInt("plant_id"));
                batch.setInteriorBatchNo(rs.getString("interior_batch_no"));
                batch.setExteriorBatchNo(rs.getString("exterior_batch_no"));
                batch.setQty(rs.getInt("qty"));

                Date date = rs.getDate("creation_date");
                if (date != null) {
                    batch.setCreationDate(date.toLocalDate());
                }

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

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM plant_batches WHERE id = ?")) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                PlantBatch batch = new PlantBatch();

                batch.setId(rs.getInt("id"));
                batch.setPlantId(rs.getInt("plant_id"));
                batch.setInteriorBatchNo(rs.getString("interior_batch_no"));
                batch.setExteriorBatchNo(rs.getString("exterior_batch_no"));
                batch.setQty(rs.getInt("qty"));

                return batch;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<PlantBatch> findByContrahentId(int contrahentId) {

        List<PlantBatch> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM plant_batches WHERE contrahent_id = ?")) {

            ps.setInt(1, contrahentId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                PlantBatch batch = new PlantBatch();

                batch.setId(rs.getInt("id"));
                batch.setPlantId(rs.getInt("plant_id"));
                batch.setInteriorBatchNo(rs.getString("interior_batch_no"));
                batch.setQty(rs.getInt("qty"));

                list.add(batch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void save(PlantBatch batch) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO plant_batches(plant_id, interior_batch_no, qty) VALUES(?,?,?)")) {

            ps.setInt(1, batch.getPlantId());
            ps.setString(2, batch.getInteriorBatchNo());
            ps.setInt(3, batch.getQty());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(PlantBatch batch) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE plant_batches SET qty=? WHERE id=?")) {

            ps.setInt(1, batch.getQty());
            ps.setInt(2, batch.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM plant_batches WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}