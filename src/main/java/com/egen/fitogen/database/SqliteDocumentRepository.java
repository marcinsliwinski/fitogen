package com.egen.fitogen.database;

import com.egen.fitogen.domain.Document;
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
                (document_number,document_type,issue_date,contrahent_id,created_by,comments)
                VALUES(?,?,?,?,?,?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            conn = getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

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
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public Document findById(int id) {

        String sql = "SELECT * FROM documents WHERE id=?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, id);

            rs = executeQuery(stmt);

            if (rs.next()) {

                Document d = new Document();

                d.setId(rs.getInt("id"));
                d.setDocumentNumber(rs.getString("document_number"));
                d.setDocumentType(rs.getString("document_type"));
                d.setIssueDate(LocalDate.parse(rs.getString("issue_date")));
                d.setContrahentId(rs.getInt("contrahent_id"));
                d.setCreatedBy(rs.getString("created_by"));
                d.setComments(rs.getString("comments"));

                return d;
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

        String sql = "SELECT * FROM documents";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);

            while (rs.next()) {

                Document d = new Document();

                d.setId(rs.getInt("id"));
                d.setDocumentNumber(rs.getString("document_number"));
                d.setDocumentType(rs.getString("document_type"));
                d.setIssueDate(LocalDate.parse(rs.getString("issue_date")));
                d.setContrahentId(rs.getInt("contrahent_id"));
                d.setCreatedBy(rs.getString("created_by"));
                d.setComments(rs.getString("comments"));

                list.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return list;
    }
}