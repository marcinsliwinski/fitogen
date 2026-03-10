package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.DocumentItem;
import com.egen.fitogen.repository.DocumentItemRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentItemRepository implements DocumentItemRepository {

    @Override
    public void save(DocumentItem item) {

        String sql = """
            INSERT INTO document_items
            (document_id, plant_batch_id, qty, passport_required)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, item.getDocumentId());
            stmt.setInt(2, item.getPlantBatchId());
            stmt.setInt(3, item.getQty());
            stmt.setBoolean(4, item.isPassportRequired());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DocumentItem> findByDocumentId(int documentId) {

        List<DocumentItem> items = new ArrayList<>();

        String sql = "SELECT * FROM document_items WHERE document_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, documentId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                DocumentItem item = new DocumentItem();

                item.setId(rs.getInt("id"));
                item.setDocumentId(rs.getInt("document_id"));
                item.setPlantBatchId(rs.getInt("plant_batch_id"));
                item.setQty(rs.getInt("qty"));
                item.setPassportRequired(rs.getBoolean("passport_required"));

                items.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }
}