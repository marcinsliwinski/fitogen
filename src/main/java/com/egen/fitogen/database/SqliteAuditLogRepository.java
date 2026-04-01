package com.egen.fitogen.database;

import com.egen.fitogen.model.AuditLogEntry;
import com.egen.fitogen.repository.AuditLogRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqliteAuditLogRepository extends BaseSqliteRepository implements AuditLogRepository {

    @Override
    public void save(AuditLogEntry entry) {
        String sql = """
                INSERT INTO audit_log (entity_type, entity_id, action_type, actor, description, changed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);

            stmt.setString(1, entry.getEntityType());
            if (entry.getEntityId() == null) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, entry.getEntityId());
            }
            stmt.setString(3, entry.getActionType());
            stmt.setString(4, entry.getActor());
            stmt.setString(5, entry.getDescription());
            stmt.setString(6, entry.getChangedAt());
            executeUpdate(stmt);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(stmt, conn);
        }
    }

    @Override
    public List<AuditLogEntry> findRecent(int limit) {
        List<AuditLogEntry> result = new ArrayList<>();
        String sql = """
                SELECT * FROM audit_log
                ORDER BY datetime(changed_at) DESC, id DESC
                LIMIT ?
                """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            stmt.setInt(1, Math.max(1, limit));
            rs = executeQuery(stmt);

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return result;
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM audit_log";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = prepare(conn, sql);
            rs = executeQuery(stmt);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rs, stmt, conn);
        }

        return 0;
    }

    private AuditLogEntry mapRow(ResultSet rs) throws Exception {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(rs.getInt("id"));

        int entityId = rs.getInt("entity_id");
        entry.setEntityId(rs.wasNull() ? null : entityId);

        entry.setEntityType(rs.getString("entity_type"));
        entry.setActionType(rs.getString("action_type"));
        entry.setActor(rs.getString("actor"));
        entry.setDescription(rs.getString("description"));
        entry.setChangedAt(rs.getString("changed_at"));
        return entry;
    }
}
