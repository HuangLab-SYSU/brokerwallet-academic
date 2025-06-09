package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.brokerfi.R;

public class ComfirmSecretActivity extends AppCompatActivity {

    private Button btn_confirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comfirm_secret);

        intView();
        intEvent();
    }

    private void intView() {
        btn_confirm = findViewById(R.id.btn_confirm);
    }

    private void intEvent(){
        btn_confirm.setOnClickListener(view -> {
            //创建意图对象
            Intent intent = new Intent();
            intent.setClass(ComfirmSecretActivity.this, CongratulationsActivity.class);
            //跳转
            startActivity(intent);
        });


    }
}