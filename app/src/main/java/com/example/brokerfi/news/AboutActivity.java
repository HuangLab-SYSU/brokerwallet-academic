package com.example.brokerfi.news;

import com.example.brokerfi.R;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.core.network.HTTPUtil;


public class AboutActivity extends AppCompatActivity {

    public static String VersionName;
    private static final int REQUEST_INSTALL_PERMISSION = 1001;
    private File mApkFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView textVersion = findViewById(R.id.text_version);
        Button btnCheckUpdate = findViewById(R.id.btn_check_update);

        // 显示版本号
        String versionName = getAppVersionName();
        VersionName = versionName;
        textVersion.setText(getString(R.string.Version,versionName));

        // 检查更新按钮点击事件
        btnCheckUpdate.setOnClickListener(v -> {
            Toast.makeText(this, R.string.about_toast_checking_for_updates, Toast.LENGTH_SHORT).show();
            
            new Thread(() -> {
                try {
                    // 从服务器获取最新版本号
                    byte[] bytes = HTTPUtil.doPost2(ApiConfig.API_ABOUT_APP_VERSION, null);
                    String response = new String(bytes);
                    JSONObject j = new JSONObject(response);
                    String latestVersion = j.getString("data");
                    
                    if (latestVersion.equals(VersionName)) {
                        runOnUiThread(() -> Toast.makeText(this, R.string.about_toast_latest_version, Toast.LENGTH_SHORT).show());
                        return;
                    }
                    
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.about_toast_update_available_version) + " " + latestVersion + getString(R.string.about_toast_downloading), Toast.LENGTH_SHORT).show());
                    
                    // 构建下载URL并下载APK
                    String downloadUrl = ApiConfig.getGithubReleaseApkUrl(latestVersion);
                    File apkFile = downloadApk(downloadUrl);
                    
                    if (apkFile != null) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.about_toast_download_completed, Toast.LENGTH_SHORT).show();
                            mApkFile = apkFile;
                            checkInstallPermission();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, R.string.about_toast_download_failed, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e("Update", "Check update failed", e);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.about_toast_update_check_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }
    
    // 下载APK文件
    private File downloadApk(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }
        
        File apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BrokerChain-Wallet.apk");
        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(apkFile);
        
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }
        
        fos.close();
        is.close();
        conn.disconnect();
        
        return apkFile;
    }
    
    // 检查安装权限
    private void checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean hasPermission = getPackageManager().canRequestPackageInstalls();
            if (hasPermission) {
                installApk(mApkFile);
            } else {
                Toast.makeText(this, R.string.about_toast_install_permission, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
            }
        } else {
            installApk(mApkFile);
        }
    }
    
    // 安装APK文件
    private void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean hasPermission = getPackageManager().canRequestPackageInstalls();
                if (hasPermission && mApkFile != null) {
                    installApk(mApkFile);
                } else {
                    Toast.makeText(this, R.string.about_toast_install_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Get app Version from Mysql in Server

    //********************************************************************************************
    // FOR Developer：Please update the latest appVersion in the database before each new release！
    //********************************************************************************************
    private String getAppVersionName() {
        // TEST MODE: Return old version to trigger update (已禁用)
        // return "1.0.0";

        // Production code (生产版本 - 已启用):
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e("Version", "Error getting version", e);
            return "Can't get AppVersion";
        }
    }

    // 打开应用在 Play 商店的页面（For update the app）
    private void openAppInPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            // Play 商店未安装，跳转网页
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(ApiConfig.getGooglePlayAppUrl(getPackageName()))));
        }
    }
}
