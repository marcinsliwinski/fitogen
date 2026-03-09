package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Document;
import com.egen.fitogen.repository.DocumentRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentRepository implements DocumentRepository {

    @Override
    public List<Document> findAll() {
        List<Document> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM documents");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDocument(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Document findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM documents WHERE id=?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToDocument(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Document> findByPlantBatchId(int plantBatchId) {
        List<Document> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM documents WHERE plant_batch_id=?")) {

            ps.setInt(1, plantBatchId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRowToDocument(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Document> findByContrahentId(int contrahentId) {
        List<Document> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM documents WHERE contrahent_id=?")) {

            ps.setInt(1, contrahentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRowToDocument(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void save(Document document) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO documents(plant_batch_id, contrahent_id, document_type, issued_by, passport, comments, creation_date) " +
                             "VALUES(?,?,?,?,?,?,?)")) {

            ps.setInt(1, document.getPlantBatchId());
            ps.setInt(2, document.getContrahentId());
            ps.setString(3, document.getDocumentType());
            ps.setString(4, document.getIssuedBy());
            ps.setBoolean(5, document.isPassport());
            ps.setString(6, document.getComments());
            ps.setDate(7, Date.valueOf(document.getCreationDate()));

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Document document) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE documents SET plant_batch_id=?, contrahent_id=?, document_type=?, issued_by=?, passport=?, comments=?, creation_date=? WHERE id=?")) {

            ps.setInt(1, document.getPlantBatchId());
            ps.setInt(2, document.getContrahentId());
            ps.setString(3, document.getDocumentType());
            ps.setString(4, document.getIssuedBy());
            ps.setBoolean(5, document.isPassport());
            ps.setString(6, document.getComments());
            ps.setDate(7, Date.valueOf(document.getCreationDate()));
            ps.setInt(8, document.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM documents WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method do mapowania ResultSet -> Document
    private Document mapRowToDocument(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getInt("id"));
        d.setPlantBatchId(rs.getInt("plant_batch_id"));
        d.setContrahentId(rs.getInt("contrahent_id"));
        d.setDocumentType(rs.getString("document_type"));
        d.setIssuedBy(rs.getString("issued_by"));
        d.setPassport(rs.getBoolean("passport"));
        d.setComments(rs.getString("comments"));

        Date date = rs.getDate("creation_date");
        if (date != null) {
            d.setCreationDate(date.toLocalDate());
        }

        return d;
    }
}