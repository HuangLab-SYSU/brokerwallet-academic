package com.example.brokerfi.notification.update;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.news.AboutActivity;

/** Coordinates the version check and the update reminder dialog. */
public final class UpdateReminderCoordinator {
    public static final String EXTRA_CHECK_UPDATE_AFTER_LOGIN =
            "com.example.brokerfi.extra.CHECK_UPDATE_AFTER_LOGIN";
    public static final String EXTRA_AUTO_START_UPDATE =
            "com.example.brokerfi.extra.AUTO_START_UPDATE";

    private static final String TAG = "UpdateReminder";

    private UpdateReminderCoordinator() {
    }

    public static void checkAndShow(AppCompatActivity activity) {
        UpdateReminderClient.fetchAvailableUpdate(activity, new ApiCallback<UpdateReminder>() {
            @Override
            public void onSuccess(UpdateReminder reminder) {
                if (reminder == null || !canShowDialog(activity)) {
                    return;
                }
                showDialog(activity, reminder);
            }

            @Override
            public void onFail(String message) {
                Log.d(TAG, message);
            }
        });
    }

    private static boolean canShowDialog(AppCompatActivity activity) {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void showDialog(
            AppCompatActivity activity,
            UpdateReminder reminder
    ) {
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_update_reminder, null, false);
        TextView currentVersion = content.findViewById(R.id.update_reminder_current_version);
        TextView latestVersion = content.findViewById(R.id.update_reminder_latest_version);
        Button updateButton = content.findViewById(R.id.update_reminder_update);
        TextView ignoreButton = content.findViewById(R.id.update_reminder_ignore);

        currentVersion.setText(activity.getString(
                R.string.update_reminder_current_version,
                reminder.getCurrentVersion()
        ));
        latestVersion.setText(activity.getString(
                R.string.update_reminder_latest_version,
                reminder.getLatestVersion()
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        ignoreButton.setOnClickListener(view -> dialog.dismiss());
        updateButton.setOnClickListener(view -> {
            dialog.dismiss();
            Intent intent = new Intent(activity, AboutActivity.class);
            intent.putExtra(EXTRA_AUTO_START_UPDATE, true);
            activity.startActivity(intent);
        });

        dialog.setOnShowListener(ignored -> resizeDialog(activity, dialog));
        dialog.show();
    }

    private static void resizeDialog(AppCompatActivity activity, AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int preferredWidth = (int) (metrics.widthPixels * 0.92f);
        int maximumWidth = Math.round(430 * metrics.density);
        window.setLayout(
                Math.min(preferredWidth, maximumWidth),
                WindowManager.LayoutParams.WRAP_CONTENT
        );
    }
}
