package com.egen.fitogen.repository;

import com.egen.fitogen.model.AuditLogEntry;

import java.util.List;

public interface AuditLogRepository {

    void save(AuditLogEntry entry);

    List<AuditLogEntry> findRecent(int limit);

    int countAll();
}
