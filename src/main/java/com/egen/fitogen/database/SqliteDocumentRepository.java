package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.Document;
import com.egen.fitogen.repository.DocumentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentRepository implements DocumentRepository {

    @Override
    public List<Document> findAll() {

        List<Document> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM documents")) {

            while (rs.next()) {

                Document d = new Document();

                d.setId(rs.getInt("id"));
                d.setContrahentId(rs.getInt("contrahent_id"));
                d.setPlantBatchId(rs.getInt("plant_batch_id"));
                d.setIssueDate(Date.valueOf(rs.getString("issue_date")).toLocalDate());
                d.setPassportRequired(rs.getInt("passport_required") == 1);
                d.setComments(rs.getString("comments"));
                d.setIssuedBy(rs.getString("issued_by"));

                list.add(d);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public Document findById(int id) { return null; }

    @Override
    public void save(Document d) {}

    @Override
    public void update(Document d) {}

    @Override
    public void delete(int id) {}
}