package com.example.brokerfi.notification.update;

/** Immutable version information shown by the update reminder dialog. */
public final class UpdateReminder {
    private final String currentVersion;
    private final String latestVersion;

    public UpdateReminder(String currentVersion, String latestVersion) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
