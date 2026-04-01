package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntry {

    private int id;
    private Integer entityId;
    private String entityType;
    private String actionType;
    private String actor;
    private String description;
    private String changedAt;
}
