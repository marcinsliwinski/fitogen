package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Document;
import com.egen.fitogen.repository.DocumentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentRepository implements DocumentRepository {

    @Override
    public void save(Document document) {

        String sql = """
                INSERT INTO documents
                (document_number, document_type, issue_date, contrahent_id, created_by, comments)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, document.getDocumentNumber());
            stmt.setString(2, document.getDocumentType());
            stmt.setString(3, document.getIssueDate().toString());
            stmt.setInt(4, document.getContrahentId());
            stmt.setString(5, document.getCreatedBy());
            stmt.setString(6, document.getComments());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                document.setId(rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document findById(int id) {

        String sql = "SELECT * FROM documents WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                Document doc = new Document();

                doc.setId(rs.getInt("id"));
                doc.setDocumentNumber(rs.getString("document_number"));
                doc.setDocumentType(rs.getString("document_type"));
                doc.setIssueDate(LocalDate.parse(rs.getString("issue_date")));
                doc.setContrahentId(rs.getInt("contrahent_id"));
                doc.setCreatedBy(rs.getString("created_by"));
                doc.setComments(rs.getString("comments"));

                return doc;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Document> findAll() {

        List<Document> documents = new ArrayList<>();

        String sql = "SELECT * FROM documents";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                Document doc = new Document();

                doc.setId(rs.getInt("id"));
                doc.setDocumentNumber(rs.getString("document_number"));
                doc.setDocumentType(rs.getString("document_type"));
                doc.setIssueDate(LocalDate.parse(rs.getString("issue_date")));
                doc.setContrahentId(rs.getInt("contrahent_id"));
                doc.setCreatedBy(rs.getString("created_by"));
                doc.setComments(rs.getString("comments"));

                documents.add(doc);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return documents;
    }
}