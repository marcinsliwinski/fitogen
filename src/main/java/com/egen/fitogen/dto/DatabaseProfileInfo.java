package com.egen.fitogen.dto;

import java.nio.file.Path;

public record DatabaseProfileInfo(
        Path databasePath,
        String displayName,
        boolean current,
        boolean testProfile,
        boolean createNewOption,
        boolean exists
) {

    public static DatabaseProfileInfo newProfileOption() {
        return new DatabaseProfileInfo(null, "Nowa baza…", false, false, true, true);
    }

    public String toDisplayLabel() {
        if (createNewOption) {
            return displayName;
        }

        StringBuilder builder = new StringBuilder(displayName == null || displayName.isBlank() ? "Baza danych" : displayName);
        if (testProfile) {
            builder.append(" • testowa");
        }
        if (!exists) {
            builder.append(" • brak pliku");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return toDisplayLabel();
    }
}
