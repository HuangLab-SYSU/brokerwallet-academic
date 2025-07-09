package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;


public class NFTMainActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private ImageView mint;
    private ImageView mynfts;
    private ImageView buy;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_main);

        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(NFTMainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        mint = findViewById(R.id.mint);
        mynfts = findViewById(R.id.mynfts);
        buy = findViewById(R.id.buy);
    }
    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this);
        mint.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(NFTMainActivity.this,MintActivity.class);
            startActivity(intent);
        });

        mynfts.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(NFTMainActivity.this, MyNFTsActivity.class);
            startActivity(intent);
        });

        buy.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(NFTMainActivity.this, BuyNFTsActivity.class);
            startActivity(intent);
        });


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
