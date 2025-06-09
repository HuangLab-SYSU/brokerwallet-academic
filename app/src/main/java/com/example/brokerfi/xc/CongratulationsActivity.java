package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.brokerfi.R;

public class CongratulationsActivity extends AppCompatActivity {
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_congratulations);
        intView();
        intEvent();
    }

    private void intView() {
        btn = findViewById(R.id.btn_done);
    }

    private void intEvent(){
        btn.setOnClickListener(view -> {
            //创建意图对象
            Intent intent = new Intent();
            intent.setClass(CongratulationsActivity.this, WelcomeBackActivity.class);
            //跳转
            startActivity(intent);
        });

    }
}