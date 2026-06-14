package com.example.brokerfi.account;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.main.MainActivity;


public class ConfirmPasswordActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String Pass = "password";
    private EditText passwordEditText;
    private Button confirmBtn;
    private Button cancelBtn;
    private String accountIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_password);

        accountIndex = getIntent().getStringExtra("accountIndex");
        intView();
        intEvent();
    }

    private void intView() {
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmBtn = findViewById(R.id.confirmBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        // Get wallet logo view
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ConfirmPasswordActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void intEvent() {
        confirmBtn.setOnClickListener(view -> {
            String inputPassword = passwordEditText.getText().toString();
            String savedPassword = getSavedPassword();

            if (savedPassword != null && savedPassword.equals(inputPassword)) {
                Intent intent = new Intent();
                intent.setClass(ConfirmPasswordActivity.this, ShowPrivateKeyActivity.class);
                intent.putExtra("accountIndex", accountIndex);
                startActivity(intent);
            } else {
                Toast.makeText(ConfirmPasswordActivity.this, R.string.confirm_password_toast_wrong_password, Toast.LENGTH_LONG).show();
            }
        });

        cancelBtn.setOnClickListener(view -> {
            finish();
        });
    }

    private String getSavedPassword() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(Pass, null);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
