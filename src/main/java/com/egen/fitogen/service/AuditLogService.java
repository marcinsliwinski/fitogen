package com.egen.fitogen.service;

import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.AuditLogEntry;
import com.egen.fitogen.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AuditLogService {

    private final AuditLogRepository repository;
    private final AppUserService appUserService;

    public AuditLogService(AuditLogRepository repository, AppUserService appUserService) {
        this.repository = repository;
        this.appUserService = appUserService;
    }

    public void log(String entityType, Integer entityId, String actionType, String description) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setEntityType(normalize(entityType));
        entry.setEntityId(entityId);
        entry.setActionType(normalize(actionType));
        entry.setActor(resolveActor());
        entry.setDescription(normalize(description));
        entry.setChangedAt(LocalDateTime.now().toString());
        repository.save(entry);
    }

    public List<AuditLogEntry> getRecentEntries(int limit) {
        return repository.findRecent(limit);
    }

    public int getEntryCount() {
        return repository.countAll();
    }


    public LocalDateTime getLatestChangedAt() {
        return getRecentEntries(1).stream()
                .map(AuditLogEntry::getChangedAt)
                .map(this::parseChangedAt)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    public String getLatestEntrySummary() {
        return getRecentEntries(1).stream()
                .findFirst()
                .map(entry -> entry.getChangedAt() + " | " + entry.getActor() + " | "
                        + entry.getEntityType() + " | " + entry.getActionType())
                .orElse("Brak wpisów audytowych.");
    }

    private String resolveActor() {
        if (appUserService == null) {
            return "System";
        }

        Integer defaultUserId = appUserService.getDefaultUserId().orElse(null);
        if (defaultUserId == null) {
            return "System";
        }

        for (AppUser user : appUserService.getAll()) {
            if (user.getId() == defaultUserId) {
                String displayName = user.getDisplayName();
                return displayName == null || displayName.isBlank() ? "System" : displayName;
            }
        }

        return "System";
    }


    private LocalDateTime parseChangedAt(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(rawValue.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
