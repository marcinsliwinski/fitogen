package com.egen.fitogen.database;

import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.repository.DocumentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentRepository
        extends BaseSqliteRepository
        implements DocumentRepository {

    @Override
    public void save(Document document) {

        String sql = """
                INSERT INTO documents
                (document_number, document_type, issue_date, contrahent_id, created_by, comments, print_passports, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, document.getDocumentNumber());
            stmt.setString(2, document.getDocumentType());
            stmt.setString(3, document.getIssueDate() != null ? document.getIssueDate().toString() : null);
            stmt.setInt(4, document.getContrahentId());
            stmt.setString(5, document.getCreatedBy());
            stmt.setString(6, document.getComments());
            stmt.setInt(7, document.isPrintPassports() ? 1 : 0);
            stmt.setString(8, resolveStatus(document).name());

            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                document.setId(rs.getInt(1));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się zapisać dokumentu.", e);
        } finally {
            close(rs, stmt, conn);
        }
    }

    @Override
    public void update(Document document) {

        String sql = """
                UPDATE documents
                SET
                    document_number = ?,
                    document_type = ?,
                    issue_date = ?,
                    contrahent_id = ?,
                    created_by = ?,
                    comments = ?,
                    print_passports = ?,
                    status = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, document.getDocumentNumber());
            stmt.setString(2, document.getDocumentType());
            stmt.setString(3, document.getIssueDate() != null ? document.getIssueDate().toString() : null);
            stmt.setInt(4, document.getContrahentId());
            stmt.setString(5, document.getCreatedBy());
            stmt.setString(6, document.getComments());
            stmt.setInt(7, document.isPrintPassports() ? 1 : 0);
            stmt.setString(8, resolveStatus(document).name());
            stmt.setInt(9, document.getId());

            executeUpdate(stmt);

        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się zaktualizować dokumentu.", e);
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public void deleteById(int id) {

        String sql = """
                UPDATE documents
                SET status = ?
                WHERE id = ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setString(1, DocumentStatus.CANCELLED.name());
            stmt.setInt(2, id);
            executeUpdate(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public Document findById(int id) {

        String sql = "SELECT * FROM documents WHERE id = ?";

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
    public List<Document> findAll() {

        List<Document> list = new ArrayList<>();

        String sql = "SELECT * FROM documents ORDER BY id DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }

    private Document mapRow(ResultSet rs) throws Exception {

        Document document = new Document();

        document.setId(rs.getInt("id"));
        document.setDocumentNumber(rs.getString("document_number"));
        document.setDocumentType(rs.getString("document_type"));

        String issueDate = rs.getString("issue_date");
        document.setIssueDate(issueDate != null && !issueDate.isBlank()
                ? LocalDate.parse(issueDate)
                : null);

        document.setContrahentId(rs.getInt("contrahent_id"));
        document.setCreatedBy(rs.getString("created_by"));
        document.setComments(rs.getString("comments"));
        document.setPrintPassports(rs.getInt("print_passports") == 1);
        document.setStatus(parseStatus(rs.getString("status")));

        return document;
    }

    private DocumentStatus parseStatus(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DocumentStatus.ACTIVE;
        }

        try {
            return DocumentStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (Exception ignored) {
            return DocumentStatus.ACTIVE;
        }
    }

    private DocumentStatus resolveStatus(Document document) {
        return document.getStatus() == null ? DocumentStatus.ACTIVE : document.getStatus();
    }
}