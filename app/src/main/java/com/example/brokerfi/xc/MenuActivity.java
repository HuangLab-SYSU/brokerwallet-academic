package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.example.brokerfi.xc.QRCode.*;
import com.example.brokerfi.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MenuActivity extends AppCompatActivity {
    private RelativeLayout sendlist;
    private RelativeLayout receivelist;
    private RelativeLayout activitylist;
    private RelativeLayout setlist;
    private RelativeLayout supportlist;
    private RelativeLayout locklist;
    private ImageView up_icon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        intView();
        intEvent();
    }

    private void intView() {
        sendlist = findViewById(R.id.sendlist);
        receivelist = findViewById(R.id.receivelist);
        activitylist = findViewById(R.id.activitylist);
        setlist = findViewById(R.id.setlist);
        supportlist = findViewById(R.id.supportlist);
        locklist = findViewById(R.id.locklist);
        up_icon = findViewById(R.id.up_icon);
    }

    private void intEvent(){
        sendlist.setOnClickListener(view -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(MenuActivity.this);
            intentIntegrator.setPrompt("For flash use volume up key");
            intentIntegrator.setBeepEnabled(true);
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.setCaptureActivity(Capture.class);
            intentIntegrator.initiateScan();
        });

        receivelist.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MenuActivity.this,ReceiveActivity.class);
            //跳转
            startActivity(intent);
        });

        activitylist.setOnClickListener(view -> {
//            Intent intent = new Intent();
//            intent.setClass(MenuActivity.this,AtvActivity.class);
//            //跳转
//            startActivity(intent);
        });

        setlist.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MenuActivity.this,SelectAccountActivity.class);
            //跳转
            startActivity(intent);
        });

        up_icon.setOnClickListener(view -> {
            finish();
        });




    }

    @Override
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