package com.egen.fitogen.database;

import com.egen.fitogen.domain.DocumentItem;
import com.egen.fitogen.repository.DocumentItemRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class SqliteDocumentItemRepository
        extends BaseSqliteRepository
        implements DocumentItemRepository {

    @Override
    public void save(DocumentItem item) {

        String sql = """
                INSERT INTO document_items
                (document_id,plant_batch_id,qty,passport_required)
                VALUES(?,?,?,?)
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
        throw new UnsupportedOperationException("Not implemented yet");
    }
}