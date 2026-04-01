package com.egen.fitogen.database;

import com.egen.fitogen.model.DocumentItem;
import com.egen.fitogen.repository.DocumentItemRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SqliteDocumentItemRepository
        extends BaseSqliteRepository
        implements DocumentItemRepository {

    @Override
    public void save(DocumentItem item) {

        String sql = """
                INSERT INTO document_items
                (document_id, plant_batch_id, qty, passport_required)
                VALUES (?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setInt(1, item.getDocumentId());
            stmt.setInt(2, item.getPlantBatchId());
            stmt.setInt(3, item.getQty());
            stmt.setBoolean(4, item.isPassportRequired());

            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public List<DocumentItem> findByDocumentId(int documentId) {

        List<DocumentItem> items = new ArrayList<>();

        String sql = """
                SELECT id, document_id, plant_batch_id, qty, passport_required
                FROM document_items
                WHERE document_id = ?
                ORDER BY id ASC
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, documentId);

            rs = executeQuery(stmt);

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
        } finally {
            close(rs, stmt, conn);
        }

        return items;
    }

    @Override
    public void deleteByDocumentId(int documentId) {

        String sql = "DELETE FROM document_items WHERE document_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, documentId);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public List<String> findActiveDocumentNumbersByPlantBatchId(int plantBatchId) {

        Set<String> numbers = new LinkedHashSet<>();

        String sql = """
                SELECT d.document_number
                FROM document_items di
                JOIN documents d ON d.id = di.document_id
                WHERE di.plant_batch_id = ?
                  AND COALESCE(d.status, 'ACTIVE') = 'ACTIVE'
                ORDER BY d.id DESC
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, plantBatchId);

            rs = executeQuery(stmt);

            while (rs.next()) {
                String number = rs.getString("document_number");
                if (number != null && !number.isBlank()) {
                    numbers.add(number);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return new ArrayList<>(numbers);
    }

    @Override
    public int sumQtyInActiveDocumentsByPlantBatchId(int plantBatchId) {
        String sql = """
                SELECT COALESCE(SUM(di.qty), 0) AS used_qty
                FROM document_items di
                JOIN documents d ON d.id = di.document_id
                WHERE di.plant_batch_id = ?
                  AND COALESCE(d.status, 'ACTIVE') = 'ACTIVE'
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, plantBatchId);
            rs = executeQuery(stmt);

            if (rs.next()) {
                return rs.getInt("used_qty");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return 0;
    }

    @Override
    public int sumQtyInActiveDocumentsByPlantBatchIdExcludingDocument(int plantBatchId, int documentId) {
        String sql = """
                SELECT COALESCE(SUM(di.qty), 0) AS used_qty
                FROM document_items di
                JOIN documents d ON d.id = di.document_id
                WHERE di.plant_batch_id = ?
                  AND d.id <> ?
                  AND COALESCE(d.status, 'ACTIVE') = 'ACTIVE'
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, plantBatchId);
            stmt.setInt(2, documentId);
            rs = executeQuery(stmt);

            if (rs.next()) {
                return rs.getInt("used_qty");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return 0;
    }
}