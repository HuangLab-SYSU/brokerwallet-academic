package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.brokerfi.R;

public class SecretPhraseActivity extends AppCompatActivity {

    private Button btn_next;
    private Button btn_remind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_phrase);
        intView();
        intEvent();
    }

    private void intView() {
        btn_next = findViewById(R.id.btn_next);
        btn_remind = findViewById(R.id.btn_remind);
    }

    private void intEvent(){
        btn_next.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SecretPhraseActivity.this, ComfirmSecretActivity.class);
            startActivity(intent);
        });

        btn_remind.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SecretPhraseActivity.this, CongratulationsActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * 处理"稍后提醒"按钮点击事件
     * 这个方法被布局文件中的android:onClick="remindMeLater"调用
     */
    public void remindMeLater(View view) {
        // 与btn_remind的点击事件相同的逻辑
        Intent intent = new Intent();
        intent.setClass(SecretPhraseActivity.this, CongratulationsActivity.class);
        startActivity(intent);
    }
}