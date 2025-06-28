package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class SettingActivity extends AppCompatActivity {

    private ImageView menu;
    private RelativeLayout action_bar;
    private RelativeLayout accountlist;
    private RelativeLayout networklist;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        accountlist = findViewById(R.id.accountlist);
        networklist = findViewById(R.id.networklist);
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this);

        accountlist.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SettingActivity.this,SelectAccountActivity.class);
            startActivity(intent);
        });

        networklist.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SettingActivity.this,SelectNetworkActivity.class);
            startActivity(intent);
        });




    }
//    public void onBackPressed() {
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
//    }
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