package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class FaucetActivity extends AppCompatActivity {

    private ImageView menu;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faucet);

        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(FaucetActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = MyUtil.claim(StorageUtil.getCurrentPrivatekey(FaucetActivity.this));
                if(result == null){
                    runOnUiThread(() -> {
                        Toast.makeText(FaucetActivity.this, "领取失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                Toast.makeText(FaucetActivity.this, "Result："+result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this);
    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode,resultCode,data
        );
        if (intentResult.getContents() != null){
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this,SendActivity.class);
            intent.putExtra("scannedData",scannedData);
            startActivity(intent);

        }
    }
}