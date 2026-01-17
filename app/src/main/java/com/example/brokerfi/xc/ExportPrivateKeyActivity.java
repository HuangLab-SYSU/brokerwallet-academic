package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExportPrivateKeyActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String Pass = "password";
    private EditText etPassword;
    private Button btnVerify;
    private Button btnCancel;
    private TextView tvWalletAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_private_key);

        intView();
        intEvent();
    }

    private void intView() {
        etPassword = findViewById(R.id.et_password);
        btnVerify = findViewById(R.id.btn_verify);
        btnCancel = findViewById(R.id.btn_cancel);
        tvWalletAddress = findViewById(R.id.tv_wallet_address);
        
        // Save path
        displayCurrentWalletAddress();
    }

    private void intEvent() {
        btnVerify.setOnClickListener(view -> {
            String inputPassword = etPassword.getText().toString();
            String savedPassword = getSavedPassword();

            if (savedPassword != null && savedPassword.equals(inputPassword)) {
                exportPrivateKeys();
            } else {
                Toast.makeText(ExportPrivateKeyActivity.this, R.string.Wrong_Password_Warning, Toast.LENGTH_LONG).show();
                finish();
            }
        });

        btnCancel.setOnClickListener(view -> {
            // Cancel
            finish();
        });
    }

    private String getSavedPassword() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(Pass, null);
    }

    private void displayCurrentWalletAddress() {
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 方法在 Android 10 版本后已经不再使用，应该改成其他方法，后续再改
        // 后续改为使用 getExternalFilesDir或getExternalCacheDir 方法
        String storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/BrokerWallet";
        //tvWalletAddress.setText("私钥将导出保存到: " + storagePath + "/private_keys_时间戳.txt");
        tvWalletAddress.setTextSize(20);
    }

    private void exportPrivateKeys() {
        String allPrivateKeys = StorageUtil.getPrivateKey(this);
        if (allPrivateKeys == null || allPrivateKeys.isEmpty()) {
            Toast.makeText(this, "No private keys found", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // Export category
            File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BrokerWallet");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Create File name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "private_keys_" + timeStamp + ".txt";
            File file = new File(exportDir, fileName);

            // Write into
            FileOutputStream fos = new FileOutputStream(file);
            fos.write("BrokerWallet Private Keys Export\n\n".getBytes());
            fos.write("Date: ".getBytes());
            fos.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()).getBytes());
            fos.write("\n\n".getBytes());

            String[] privateKeys = allPrivateKeys.split(";");
            for (int i = 0; i < privateKeys.length; i++) {
                String privateKey = privateKeys[i];
                String address = SecurityUtil.GetAddress(privateKey);
                
                fos.write("Account ".getBytes());
                fos.write(String.valueOf(i + 1).getBytes());
                fos.write("\n".getBytes());
                fos.write("Address: ".getBytes());
                fos.write(address.getBytes());
                fos.write("\n".getBytes());
                fos.write("Private Key: ".getBytes());
                fos.write(privateKey.getBytes());
                fos.write("\n\n".getBytes());
            }

            fos.close();

            // SUCCESS TOAST TEXT
            String message = "PK exported to " + exportDir.getAbsolutePath();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export private keys", Toast.LENGTH_LONG).show();
        }
    }
}