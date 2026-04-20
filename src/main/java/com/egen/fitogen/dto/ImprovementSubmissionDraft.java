package com.egen.fitogen.dto;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImprovementSubmissionDraft {

    private final String type;
    private final String title;
    private final String description;
    private final String expectedBenefit;
    private final String priority;
    private final List<Path> attachments;
    private final String fallbackEmail;
    private final String appVersion;
    private final String osName;
    private final String javaVersion;
    private final String databaseProfile;
    private final String databasePath;
    private final String issuerName;
    private final LocalDateTime createdAt;

    public ImprovementSubmissionDraft(String type,
                                      String title,
                                      String description,
                                      String expectedBenefit,
                                      String priority,
                                      List<Path> attachments,
                                      String fallbackEmail,
                                      String appVersion,
                                      String osName,
                                      String javaVersion,
                                      String databaseProfile,
                                      String databasePath,
                                      String issuerName,
                                      LocalDateTime createdAt) {
        this.type = safe(type);
        this.title = safe(title);
        this.description = safe(description);
        this.expectedBenefit = safe(expectedBenefit);
        this.priority = safe(priority);
        this.attachments = attachments == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(attachments));
        this.fallbackEmail = safe(fallbackEmail);
        this.appVersion = safe(appVersion);
        this.osName = safe(osName);
        this.javaVersion = safe(javaVersion);
        this.databaseProfile = safe(databaseProfile);
        this.databasePath = safe(databasePath);
        this.issuerName = safe(issuerName);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getExpectedBenefit() {
        return expectedBenefit;
    }

    public String getPriority() {
        return priority;
    }

    public List<Path> getAttachments() {
        return attachments;
    }

    public String getFallbackEmail() {
        return fallbackEmail;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getDatabaseProfile() {
        return databaseProfile;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendJsonField(builder, "type", type, true);
        appendJsonField(builder, "title", title, true);
        appendJsonField(builder, "description", description, true);
        appendJsonField(builder, "expectedBenefit", expectedBenefit, true);
        appendJsonField(builder, "priority", priority, true);
        appendJsonField(builder, "fallbackEmail", fallbackEmail, true);
        appendJsonField(builder, "appVersion", appVersion, true);
        appendJsonField(builder, "osName", osName, true);
        appendJsonField(builder, "javaVersion", javaVersion, true);
        appendJsonField(builder, "databaseProfile", databaseProfile, true);
        appendJsonField(builder, "databasePath", databasePath, true);
        appendJsonField(builder, "issuerName", issuerName, true);
        appendJsonField(builder, "createdAt", createdAt.toString(), true);
        builder.append("  \"attachments\": [");
        if (!attachments.isEmpty()) {
            builder.append("\n");
            for (int i = 0; i < attachments.size(); i++) {
                builder.append("    \"").append(escapeJson(attachments.get(i).toAbsolutePath().normalize().toString())).append("\"");
                if (i < attachments.size() - 1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append("  ");
        }
        builder.append("]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendJsonField(StringBuilder builder, String key, String value, boolean comma) {
        builder.append("  \"").append(escapeJson(key)).append("\": \"")
                .append(escapeJson(value))
                .append("\"");
        if (comma) {
            builder.append(",");
        }
        builder.append("\n");
    }

    private String escapeJson(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
