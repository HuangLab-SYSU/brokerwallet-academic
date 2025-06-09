package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.example.brokerfi.R;

public class ImportActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT_NUMBER = "accountNumber";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        String address = getAccountNumberFromSharedPreferences();
        if (address != null){

            Intent intent = new Intent(ImportActivity.this, WelcomeBackActivity.class);
            startActivity(intent);
        }
    }


    private String getAccountNumberFromSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_ACCOUNT_NUMBER, null); // 如果没有找到，返回null
    }

}