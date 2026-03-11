package com.egen.fitogen.database;

import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.repository.PlantBatchRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantBatchRepository
        extends BaseSqliteRepository
        implements PlantBatchRepository {

    @Override
    public List<PlantBatch> findAll() {
        List<PlantBatch> list = new ArrayList<>();
        String sql = "SELECT * FROM plant_batches";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {
                PlantBatch batch = mapRow(rs);
                list.add(batch);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    @Override
    public PlantBatch findById(int id) {
        String sql = "SELECT * FROM plant_batches WHERE id=?";
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
    public List<PlantBatch> findByContrahentId(int contrahentId) {
        List<PlantBatch> list = new ArrayList<>();
        String sql = "SELECT * FROM plant_batches WHERE contrahent_id=?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, contrahentId);
            rs = executeQuery(stmt);

            while (rs.next()) {
                PlantBatch batch = mapRow(rs);
                list.add(batch);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    @Override
    public void save(PlantBatch batch) {
        String sql = """
                INSERT INTO plant_batches
                (interior_batch_no, exterior_batch_no, plant_id, qty, creation_date,
                 manufacturer_country_code, fito_qualification_category,
                 eppo_code, zp_zone, contrahent_id, comments)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, batch.getInteriorBatchNo());
            stmt.setString(2, batch.getExteriorBatchNo());
            stmt.setInt(3, batch.getPlantId());
            stmt.setInt(4, batch.getQty());

            if (batch.getCreationDate() != null) {
                stmt.setDate(5, Date.valueOf(batch.getCreationDate()));
            } else {
                stmt.setDate(5, null);
            }

            stmt.setString(6, batch.getManufacturerCountryCode());
            stmt.setString(7, batch.getFitoQualificationCategory());
            stmt.setString(8, batch.getEppoCode());
            stmt.setString(9, batch.getZpZone());
            stmt.setInt(10, batch.getContrahentId());
            stmt.setString(11, batch.getComments());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(PlantBatch batch) {
        String sql = """
                UPDATE plant_batches
                SET interior_batch_no=?, exterior_batch_no=?, plant_id=?, qty=?, creation_date=?,
                    manufacturer_country_code=?, fito_qualification_category=?, eppo_code=?,
                    zp_zone=?, contrahent_id=?, comments=?
                WHERE id=?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, batch.getInteriorBatchNo());
            stmt.setString(2, batch.getExteriorBatchNo());
            stmt.setInt(3, batch.getPlantId());
            stmt.setInt(4, batch.getQty());

            if (batch.getCreationDate() != null) {
                stmt.setDate(5, Date.valueOf(batch.getCreationDate()));
            } else {
                stmt.setDate(5, null);
            }

            stmt.setString(6, batch.getManufacturerCountryCode());
            stmt.setString(7, batch.getFitoQualificationCategory());
            stmt.setString(8, batch.getEppoCode());
            stmt.setString(9, batch.getZpZone());
            stmt.setInt(10, batch.getContrahentId());
            stmt.setString(11, batch.getComments());
            stmt.setInt(12, batch.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM plant_batches WHERE id=?";
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

    private PlantBatch mapRow(ResultSet rs) throws Exception {
        PlantBatch batch = new PlantBatch();

        batch.setId(rs.getInt("id"));
        batch.setInteriorBatchNo(rs.getString("interior_batch_no"));
        batch.setExteriorBatchNo(rs.getString("exterior_batch_no"));
        batch.setPlantId(rs.getInt("plant_id"));
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

        return batch;
    }
}