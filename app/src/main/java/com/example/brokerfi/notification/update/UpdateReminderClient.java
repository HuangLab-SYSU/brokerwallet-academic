package com.example.brokerfi.notification.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.HTTPUtil;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Retrieves the latest app version and determines whether an update is available. */
public final class UpdateReminderClient {
    private UpdateReminderClient() {
    }

    public static void fetchAvailableUpdate(
            Context context,
            ApiCallback<UpdateReminder> callback
    ) {
        String currentVersion = getCurrentVersion(context);

        new Thread(() -> {
            try {
                byte[] bytes = HTTPUtil.doPost2(ApiConfig.API_ABOUT_APP_VERSION, null);
                JSONObject response = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                String latestVersion = response.optString("data", "").trim();
                UpdateReminder reminder = isNewerVersion(latestVersion, currentVersion)
                        ? new UpdateReminder(currentVersion, latestVersion)
                        : null;
                postToMainThread(() -> callback.onSuccess(reminder));
            } catch (Exception error) {
                postToMainThread(() -> callback.onFail(
                        "Update reminder check failed: " + error.getMessage()
                ));
            }
        }, "update-reminder-check").start();
    }

    public static boolean isNewerVersion(String candidate, String current) {
        long[] candidateParts = parseVersion(candidate);
        long[] currentParts = parseVersion(current);
        if (candidateParts == null || currentParts == null) {
            return false;
        }

        int count = Math.max(candidateParts.length, currentParts.length);
        for (int index = 0; index < count; index++) {
            long candidatePart = index < candidateParts.length ? candidateParts[index] : 0;
            long currentPart = index < currentParts.length ? currentParts[index] : 0;
            if (candidatePart != currentPart) {
                return candidatePart > currentPart;
            }
        }
        return false;
    }

    private static long[] parseVersion(String version) {
        if (version == null) {
            return null;
        }

        String normalized = version.trim();
        if (normalized.startsWith("V") || normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }

        String[] values = normalized.split("\\.", -1);
        long[] parts = new long[values.length];
        for (int index = 0; index < values.length; index++) {
            if (!values[index].matches("\\d+")) {
                return null;
            }
            try {
                parts[index] = Long.parseLong(values[index]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return parts;
    }

    private static String getCurrentVersion(Context context) {
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
            return fallback(version, "Unknown");
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    private static String fallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static void postToMainThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
