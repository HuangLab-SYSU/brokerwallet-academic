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
    private Button btn_toggle_password;
    private TextView txw_tip;
    private boolean isPasswordVisible = false;

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
        btn_unlock = findViewById(R.id.btn_unlock);
        btn_toggle_password = findViewById(R.id.btn_toggle_password);
        //txw_tip = findViewById(R.id.txw_tip);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.welcome_back_message_confirm_exit_app)
                .setCancelable(false)
                .setPositiveButton(R.string.welcome_back_button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                    }
                })
                .setNegativeButton(R.string.welcome_back_button_no, null)
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
                Toast.makeText(WelcomeBackActivity.this, R.string.confirm_password_toast_wrong_password, Toast.LENGTH_LONG).show();
            }


        });

        btn_toggle_password.setOnClickListener(view -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                edt_passw.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                btn_toggle_password.setBackgroundResource(R.drawable.ic_eye_closed);
            } else {
                edt_passw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btn_toggle_password.setBackgroundResource(R.drawable.ic_eye_open);
            }
            edt_passw.setSelection(edt_passw.getText().length());
        });

    }
}