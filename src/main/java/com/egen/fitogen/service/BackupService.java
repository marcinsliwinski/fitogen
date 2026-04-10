package com.egen.fitogen.service;

import com.egen.fitogen.config.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class BackupService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public Path createBackup(Path targetDirectory) {
        if (targetDirectory == null) {
            throw new IllegalArgumentException("Katalog docelowy backupu jest wymagany.");
        }
        if (!Files.exists(targetDirectory)) {
            throw new IllegalArgumentException("Wybrany katalog nie istnieje.");
        }
        if (!Files.isDirectory(targetDirectory)) {
            throw new IllegalArgumentException("Wybrana ścieżka nie jest katalogiem.");
        }

        Path sourceDbPath = DatabaseConfig.getDatabaseFilePath();
        if (!Files.exists(sourceDbPath)) {
            throw new IllegalStateException("Nie znaleziono pliku bazy danych: " + sourceDbPath);
        }

        Path targetFile = targetDirectory.resolve(buildBackupFileName());
        try {
            Files.copy(sourceDbPath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            DatabaseConfig.rememberBackup(targetFile);
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się utworzyć backupu bazy danych.", e);
        }
    }

    public Path getDatabaseFilePath() {
        return DatabaseConfig.getDatabaseFilePath();
    }

    public static Optional<Path> getRememberedBackupPath() {
        return DatabaseConfig.getRememberedBackupPath();
    }

    private String buildBackupFileName() {
        String databaseFileName = DatabaseConfig.getDatabaseFilePath().getFileName() == null
                ? "fitogen"
                : DatabaseConfig.getDatabaseFilePath().getFileName().toString().replaceFirst("\\.db$", "");
        return databaseFileName + "_backup_" + LocalDateTime.now().format(FORMATTER) + ".db";
    }
}
