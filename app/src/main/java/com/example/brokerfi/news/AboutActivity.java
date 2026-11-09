package com.example.brokerfi.news;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.brokerfi.R;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.core.network.HTTPUtil;
import com.example.brokerfi.notification.update.UpdateReminderClient;
import com.example.brokerfi.notification.update.UpdateReminderCoordinator;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AboutActivity extends AppCompatActivity {
    private static final String TAG = "AboutActivity";
    private static final int REQUEST_INSTALL_PERMISSION = 1001;
    private static final String APK_FILE_NAME = "BrokerChain-Wallet.apk";

    private File downloadedApkFile;
    private String currentVersion;
    private Button checkUpdateButton;
    private ProgressBar updateProgress;
    private TextView updateStatus;
    private View updateStatusContainer;
    private boolean autoStartUpdate;
    private boolean updateInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView versionText = findViewById(R.id.text_version);
        checkUpdateButton = findViewById(R.id.btn_check_update);
        updateProgress = findViewById(R.id.update_progress);
        updateStatus = findViewById(R.id.update_status);
        updateStatusContainer = findViewById(R.id.update_status_container);

        currentVersion = getAppVersionName();
        versionText.setText(getString(R.string.Version, currentVersion));
        checkUpdateButton.setOnClickListener(view -> startUpdateFlow());

        autoStartUpdate = getIntent().getBooleanExtra(
                UpdateReminderCoordinator.EXTRA_AUTO_START_UPDATE,
                false
        );
        if (autoStartUpdate) {
            getIntent().removeExtra(UpdateReminderCoordinator.EXTRA_AUTO_START_UPDATE);
            checkUpdateButton.setVisibility(View.GONE);
            checkUpdateButton.post(this::startUpdateFlow);
        }
    }

    private void startUpdateFlow() {
        if (updateInProgress) {
            return;
        }

        updateInProgress = true;
        checkUpdateButton.setEnabled(false);
        if (autoStartUpdate) {
            checkUpdateButton.setVisibility(View.GONE);
        }
        showUpdateStatus(getString(R.string.about_status_checking), true);
        if (!autoStartUpdate) {
            Toast.makeText(
                    this,
                    R.string.about_toast_checking_for_updates,
                    Toast.LENGTH_SHORT
            ).show();
        }

        new Thread(() -> {
            try {
                byte[] bytes = HTTPUtil.doPost2(ApiConfig.API_ABOUT_APP_VERSION, null);
                JSONObject response = new JSONObject(
                        new String(bytes, StandardCharsets.UTF_8)
                );
                String latestVersion = response.optString("data", "").trim();

                if (!UpdateReminderClient.isNewerVersion(latestVersion, currentVersion)) {
                    runOnUiThreadIfActive(this::showLatestVersionState);
                    return;
                }

                runOnUiThreadIfActive(() -> showUpdateStatus(
                        getString(R.string.about_status_downloading, latestVersion),
                        true
                ));

                String downloadUrl = ApiConfig.getGithubReleaseApkUrl(latestVersion);
                File apkFile = downloadApk(downloadUrl);
                if (apkFile == null) {
                    runOnUiThreadIfActive(() -> showRetryState(
                            getString(R.string.about_status_download_failed)
                    ));
                    return;
                }

                runOnUiThreadIfActive(() -> {
                    downloadedApkFile = apkFile;
                    showUpdateStatus(
                            getString(R.string.about_status_opening_installer),
                            false
                    );
                    checkInstallPermission();
                });
            } catch (Exception error) {
                Log.e(TAG, "Update check failed", error);
                runOnUiThreadIfActive(() -> showRetryState(
                        getString(R.string.about_status_check_failed)
                ));
            }
        }, "about-update-flow").start();
    }

    private void showLatestVersionState() {
        updateInProgress = false;
        showUpdateStatus(getString(R.string.about_status_latest), false);
        if (!autoStartUpdate) {
            checkUpdateButton.setText(R.string.activity_about_check_for_updates);
            checkUpdateButton.setEnabled(true);
            checkUpdateButton.setVisibility(View.VISIBLE);
        }
    }

    private void showRetryState(String message) {
        updateInProgress = false;
        showUpdateStatus(message, false);
        checkUpdateButton.setText(R.string.about_button_retry_update);
        checkUpdateButton.setEnabled(true);
        checkUpdateButton.setVisibility(View.VISIBLE);
    }

    private void showUpdateStatus(String message, boolean showProgress) {
        updateStatusContainer.setVisibility(View.VISIBLE);
        updateStatus.setText(message);
        updateProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    private void runOnUiThreadIfActive(Runnable action) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                action.run();
            }
        });
    }

    private File downloadApk(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            File apkFile = getDownloadedApkFile();
            if (apkFile == null) {
                return null;
            }
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(apkFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
            }
            return apkFile;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private File getDownloadedApkFile() {
        File downloadsDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDirectory == null) {
            return null;
        }
        return new File(downloadsDirectory, APK_FILE_NAME);
    }

    private void checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(
                    this,
                    R.string.about_toast_install_permission,
                    Toast.LENGTH_LONG
            ).show();
            showUpdateStatus(
                    getString(R.string.about_status_waiting_install_permission),
                    false
            );
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
            return;
        }
        installApk(downloadedApkFile);
    }

    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.isFile()) {
            showRetryState(getString(R.string.about_status_download_failed));
            return;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    apkFile
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            updateInProgress = false;
            startActivity(intent);
        } catch (Exception error) {
            Log.e(TAG, "Unable to open APK installer", error);
            showRetryState(getString(R.string.about_status_open_installer_failed));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_INSTALL_PERMISSION || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (!getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(
                    this,
                    R.string.about_toast_install_permission_denied,
                    Toast.LENGTH_SHORT
            ).show();
            showRetryState(getString(R.string.about_status_install_permission_denied));
            return;
        }

        if (downloadedApkFile == null) {
            downloadedApkFile = getDownloadedApkFile();
        }
        showUpdateStatus(getString(R.string.about_status_opening_installer), false);
        installApk(downloadedApkFile);
    }

    private String getAppVersionName() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
            return versionName == null
                    ? getString(R.string.about_status_can_t_get_appversion)
                    : versionName;
        } catch (Exception error) {
            Log.e(TAG, "Error getting app version", error);
            return getString(R.string.about_status_can_t_get_appversion);
        }
    }

    @SuppressWarnings("unused")
    private void openAppInPlayStore() {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())
            ));
        } catch (android.content.ActivityNotFoundException error) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(ApiConfig.getGooglePlayAppUrl(getPackageName()))
            ));
        }
    }
}
