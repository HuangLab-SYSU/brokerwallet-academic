package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CreatePasswActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT_NUMBER = "accountNumber";
    private static final String Pass = "password";
    private EditText new_passw;
    private EditText confirm_passw;
    private CheckBox checkBox;
    private Button btn_create;
    private TextView txw_underline;
    private SpannableString spannableString = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_passw);
        intView();
        intEvent();

        String address = getAccountNumberFromSharedPreferences();
        if (address != null) {
            Intent intent = new Intent(CreatePasswActivity.this, WelcomeBackActivity.class);
            startActivity(intent);
        }
    }

    private String getAccountNumberFromSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_ACCOUNT_NUMBER, null); // 如果没有找到，返回null
    }


    private void intView() {
        new_passw = findViewById(R.id.new_passw);
        confirm_passw = findViewById(R.id.confirm_passw);
        checkBox = findViewById(R.id.checkbox);
        btn_create = findViewById(R.id.btn_create);
        txw_underline = findViewById(R.id.txw_underline);
    }

    private void intEvent() {

        String text = "Terms of use";
        spannableString = new SpannableString(text);
        spannableString.setSpan(new UnderlineSpan(), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        txw_underline.setText(spannableString);


        btn_create.setOnClickListener(view -> {
            String newPassw = String.valueOf(new_passw.getText());
            String confirnPassw = String.valueOf(confirm_passw.getText());
            if (!checkBox.isChecked()) {
                Toast.makeText(CreatePasswActivity.this, "请同意协议！", Toast.LENGTH_LONG).show();
                return;
            }
            if (newPassw == "") {
                Toast.makeText(CreatePasswActivity.this, "密码不得为空！", Toast.LENGTH_LONG).show();
                return;
            }
            if (newPassw.length() < 6) {
                Toast.makeText(CreatePasswActivity.this, "密码最少6位！", Toast.LENGTH_LONG).show();
                return;
            }
            if (!newPassw.equals(confirnPassw)) {
                Toast.makeText(CreatePasswActivity.this, "密码和确认密码不同！", Toast.LENGTH_LONG).show();
                return;
            }

            String address = generateAddress(newPassw + System.currentTimeMillis(), 40);
            System.out.println("create address: " + address);
            saveAccountNumberToSharedPreferences(address);
            savePasswd(newPassw);
            //创建意图对象
            Intent intent = new Intent();
            intent.setClass(CreatePasswActivity.this, CongratulationsActivity.class);
            //跳转
            startActivity(intent);


        });
    }

    private void savePasswd(String accountNumber) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Pass, accountNumber);
        editor.apply();
    }

    private void saveAccountNumberToSharedPreferences(String accountNumber) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NUMBER, accountNumber);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        // 在这里执行你的自定义操作
        super.onBackPressed(); // 调用父类的方法以保持默认的返回行为
    }

    public static String generateAddress(String input, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            BigInteger number = new BigInteger(1, hash);
            String hashedText = number.toString(16);
            if (hashedText.length() > length) {
                return hashedText.substring(0, length);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < length - hashedText.length(); i++) {
                    sb.append("0");
                }
                return sb.toString() + hashedText;
            }
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


}