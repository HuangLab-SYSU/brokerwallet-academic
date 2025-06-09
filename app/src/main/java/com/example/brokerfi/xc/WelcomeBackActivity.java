package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;

public class WelcomeBackActivity extends AppCompatActivity {

    private EditText edt_passw;
    private Button btn_unlock;
    private TextView txw_tip;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String Pass = "password";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_back);
        intView();
        intEvent();
    }
    private String getPassword() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(Pass, null);
    }
    private void intView() {
        edt_passw = findViewById(R.id.edt_passw);
        btn_unlock = (Button) findViewById(R.id.btn_unlock);
        txw_tip = findViewById(R.id.txw_tip);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("你确定要退出应用吗？")
                .setCancelable(false)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                    }
                })
                .setNegativeButton("否", null)
                .show();
    }

    private void intEvent(){
        btn_unlock.setOnClickListener(view -> {
            String password = getPassword();
            if(password==null){
                return;
            }


            if(getPassword().equals(edt_passw.getText().toString())){

                Intent intent = new Intent();
                intent.setClass(WelcomeBackActivity.this, MainActivity.class);

                startActivity(intent);
            }else{
                Toast.makeText(WelcomeBackActivity.this, "密码错误！", Toast.LENGTH_LONG).show();
            }


        });

    }
}