package com.egen.fitogen.dto;

import java.nio.file.Path;

public class ImprovementSubmissionResult {

    private final String channel;
    private final String message;
    private final Path draftFile;
    private final boolean openedExternalClient;

    public ImprovementSubmissionResult(String channel, String message, Path draftFile, boolean openedExternalClient) {
        this.channel = channel == null ? "" : channel.trim();
        this.message = message == null ? "" : message.trim();
        this.draftFile = draftFile;
        this.openedExternalClient = openedExternalClient;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public Path getDraftFile() {
        return draftFile;
    }

    public boolean isOpenedExternalClient() {
        return openedExternalClient;
    }
}
