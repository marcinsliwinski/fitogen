package com.egen.fitogen.dto;

import java.nio.file.Path;

public record DatabaseProfileInfo(
        Path databasePath,
        String displayName,
        boolean testProfile,
        boolean createdNow
) {
}
