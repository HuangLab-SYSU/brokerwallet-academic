package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.brokerfi.R;

public class WelcomeActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT_NUMBER = "accountNumber";
    private Button btn_createWallet;
    private String getAccountNumberFromSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_ACCOUNT_NUMBER, null);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        intView();
        intEvent();
        String address = getAccountNumberFromSharedPreferences();
        if (address != null) {

            Intent intent = new Intent(WelcomeActivity.this, WelcomeBackActivity.class);
            startActivity(intent);
        }

    }

    private void intView() {
        btn_createWallet = findViewById(R.id.createWalletButton);
    }

    private void intEvent(){
        btn_createWallet.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(WelcomeActivity.this, CreatePasswActivity.class);

            startActivity(intent);
        });
        Context context = this;
        dp(context);
    }
    private int dp(Context context){
        float scale = context.getResources().getDisplayMetrics().density;
        Log.d("density", String.valueOf(scale));
        return 0;
    }
}

