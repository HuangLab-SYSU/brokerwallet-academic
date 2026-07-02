package com.example.brokerfi.account;

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
            // Create intent object
            Intent intent = new Intent();
            intent.setClass(ComfirmSecretActivity.this, CongratulationsActivity.class);
            // Navigate
            startActivity(intent);
        });


    }
}
