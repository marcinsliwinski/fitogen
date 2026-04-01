package com.egen.fitogen.database;

import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.repository.DocumentTypeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentTypeRepository
        extends BaseSqliteRepository
        implements DocumentTypeRepository {

    @Override
    public List<DocumentType> findAll() {
        List<DocumentType> list = new ArrayList<>();
        String sql = "SELECT * FROM document_types ORDER BY name COLLATE NOCASE";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {
                DocumentType type = new DocumentType();
                type.setId(rs.getInt("id"));
                type.setName(rs.getString("name"));
                type.setCode(rs.getString("code"));
                list.add(type);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    @Override
    public void save(DocumentType documentType) {
        String sql = "INSERT INTO document_types (name, code) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, documentType.getName());
            stmt.setString(2, documentType.getCode());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void update(DocumentType documentType) {
        String sql = "UPDATE document_types SET name = ?, code = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, documentType.getName());
            stmt.setString(2, documentType.getCode());
            stmt.setInt(3, documentType.getId());
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM document_types WHERE id = ?";

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